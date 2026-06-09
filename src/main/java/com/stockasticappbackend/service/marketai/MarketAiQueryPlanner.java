package com.stockasticappbackend.service.marketai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockasticappbackend.dto.marketai.AiQueryAction;
import com.stockasticappbackend.dto.marketai.AiQueryPlan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketAiQueryPlanner {

    private static final Pattern TIME_PATTERN =
            Pattern.compile("\\b(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*(am|pm)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern SYMBOL_AFTER_OF_PATTERN =
            Pattern.compile("\\bof\\s+([A-Z][A-Z0-9.-]{1,19})\\b");

    private static final Pattern SYMBOL_AFTER_COMPARE_PATTERN =
            Pattern.compile("\\bcompare\\s+([A-Z][A-Z0-9.-]{1,19})\\s*(with|vs\\.?|and)\\s+([A-Z][A-Z0-9.-]{1,19})\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern STOCK_TIME_COMPARISON_PATTERN =
            Pattern.compile(
                    "\\b([A-Z][A-Z0-9.-]{1,19})\\b(?:\\s+price)?\\s+at\\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)?)\\s+(?:and|vs\\.?|with|compare\\s+to)\\s+([A-Z][A-Z0-9.-]{1,19})\\b(?:\\s+price)?\\s+at\\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern MULTI_STOCK_COMPARE_PATTERN =
            Pattern.compile("\\bcompare\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern SYMBOL_TOKEN_PATTERN =
            Pattern.compile("\\b[A-Z][A-Z0-9.-]{1,19}\\b");

    private static final Pattern HIGHEST_AFTER_TIME_PATTERN =
            Pattern.compile(
                    "(?:\\bhighest\\b|\\bmax\\b).*(?:\\bof\\b\\s+)?([A-Z][A-Z0-9.-]{1,19})\\b.*\\bafter\\b\\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)?)|\\b([A-Z][A-Z0-9.-]{1,19})\\b.*(?:\\bhighest\\b|\\bmax\\b).*\\bafter\\b\\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern SECTOR_MOVERS_PATTERN =
            Pattern.compile(
                    "\\btop\\s+(gainers|losers|movers)\\b.*\\bsector\\b\\s+([A-Za-z][A-Za-z\\s&-]{1,40})",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern GENERAL_MOVERS_PATTERN =
            Pattern.compile("\\btop\\s+(gainers|losers|movers)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern VOLUME_BETWEEN_TIMES_PATTERN =
            Pattern.compile(
                    "\\bvolume\\b.*\\bof\\b\\s+([A-Z][A-Z0-9.-]{1,19})\\b.*\\b(?:between|from)\\b\\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)?)\\s+\\b(?:and|to)\\b\\s+((?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern TOP_VOLUME_PATTERN =
            Pattern.compile(
                    "\\b(top|highest)\\s+volume\\b(?:.*\\bsector\\b\\s+([A-Za-z][A-Za-z\\s&-]{1,40}))?",
                    Pattern.CASE_INSENSITIVE);

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;

    public AiQueryPlan plan(String query, String requestedSymbol) {
        AiQueryPlan fallback = buildFallbackPlan(query, requestedSymbol);

        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            return fallback;
        }

        try {
            String content = chatClientBuilder.build()
                    .prompt()
                    .system("""
                            Convert a stock-market user question into a strict JSON plan.
                            Allowed actions:
                            - SNAPSHOT
                            - PRICE_AT_TIME
                            - COMPARE_PRICES
                            - COMPARE_STOCKS
                            - COMPARE_STOCKS_AT_TIMES
                            - COMPARE_MULTIPLE_STOCKS
                            - HIGHEST_AFTER_TIME
                            - TOP_MOVERS_IN_SECTOR
                            - VOLUME_QUERY

                            Rules:
                            - Output JSON only. No markdown.
                            - For PRICE_AT_TIME: include symbol and time.
                            - For COMPARE_PRICES: include symbol, time, comparisonTime.
                            - For COMPARE_STOCKS: include symbol and comparisonSymbol.
                            - For COMPARE_STOCKS_AT_TIMES: include symbol, time, comparisonSymbol, comparisonSymbolTime.
                            - For COMPARE_MULTIPLE_STOCKS: include symbols array.
                            - For HIGHEST_AFTER_TIME: include symbol and time.
                            - For TOP_MOVERS_IN_SECTOR: include sector and optional direction as gainers/losers/movers.
                            - For VOLUME_QUERY: include either symbol+time+comparisonTime or sector when useful.
                            - For SNAPSHOT: use when the user asks a general market question.
                            - time fields must be in 24-hour HH:mm format when known.
                            - Never invent fields beyond: action, symbol, comparisonSymbol, time, comparisonTime, comparisonSymbolTime, symbols, sector, direction, rationale.
                            """)
                    .user("""
                            Requested symbol hint: %s
                            User question: %s
                            """.formatted(requestedSymbol == null ? "none" : requestedSymbol, query))
                    .call()
                    .content();

            AiQueryPlan parsed = objectMapper.readValue(stripCodeFences(content), AiQueryPlan.class);
            normalizePlan(parsed, requestedSymbol);
            return parsed.getAction() == null ? fallback : parsed;
        } catch (Exception ex) {
            log.debug("Falling back to heuristic AI plan for query '{}': {}", query, ex.getMessage());
            return fallback;
        }
    }

    private AiQueryPlan buildFallbackPlan(String query, String requestedSymbol) {
        AiQueryPlan plan = new AiQueryPlan();
        String normalized = query == null ? "" : query.trim();
        plan.setSymbol(normalizeSymbol(requestedSymbol));

        Matcher stockTimeComparisonMatcher = STOCK_TIME_COMPARISON_PATTERN.matcher(normalized.toUpperCase(Locale.ROOT));
        if (stockTimeComparisonMatcher.find()) {
            plan.setAction(AiQueryAction.COMPARE_STOCKS_AT_TIMES);
            plan.setSymbol(normalizeSymbol(stockTimeComparisonMatcher.group(1)));
            plan.setTime(normalizeTimeToken(stockTimeComparisonMatcher.group(2)));
            plan.setComparisonSymbol(normalizeSymbol(stockTimeComparisonMatcher.group(3)));
            plan.setComparisonSymbolTime(normalizeTimeToken(stockTimeComparisonMatcher.group(4)));
            return plan;
        }

        Matcher stockCompareMatcher = SYMBOL_AFTER_COMPARE_PATTERN.matcher(normalized);
        if (stockCompareMatcher.find()) {
            plan.setAction(AiQueryAction.COMPARE_STOCKS);
            plan.setSymbol(normalizeSymbol(stockCompareMatcher.group(1)));
            plan.setComparisonSymbol(normalizeSymbol(stockCompareMatcher.group(3)));
            return plan;
        }

        if (MULTI_STOCK_COMPARE_PATTERN.matcher(normalized).find()) {
            List<String> symbols = extractSymbols(normalized.toUpperCase(Locale.ROOT));
            if (symbols.size() >= 3) {
                plan.setAction(AiQueryAction.COMPARE_MULTIPLE_STOCKS);
                plan.setSymbols(symbols);
                return plan;
            }
        }

        Matcher highestAfterMatcher = HIGHEST_AFTER_TIME_PATTERN.matcher(normalized.toUpperCase(Locale.ROOT));
        if (highestAfterMatcher.find()) {
            String symbol = highestAfterMatcher.group(1) != null ? highestAfterMatcher.group(1) : highestAfterMatcher.group(3);
            String time = highestAfterMatcher.group(2) != null ? highestAfterMatcher.group(2) : highestAfterMatcher.group(4);
            plan.setAction(AiQueryAction.HIGHEST_AFTER_TIME);
            plan.setSymbol(normalizeSymbol(symbol));
            plan.setTime(normalizeTimeToken(time));
            return plan;
        }

        Matcher sectorMoversMatcher = SECTOR_MOVERS_PATTERN.matcher(normalized);
        if (sectorMoversMatcher.find()) {
            plan.setAction(AiQueryAction.TOP_MOVERS_IN_SECTOR);
            plan.setDirection(sectorMoversMatcher.group(1).toLowerCase(Locale.ROOT));
            plan.setSector(normalizeSector(sectorMoversMatcher.group(2)));
            return plan;
        }

        Matcher generalMoversMatcher = GENERAL_MOVERS_PATTERN.matcher(normalized);
        if (generalMoversMatcher.find()) {
            plan.setAction(AiQueryAction.TOP_MOVERS_IN_SECTOR);
            plan.setDirection(generalMoversMatcher.group(1).toLowerCase(Locale.ROOT));
            return plan;
        }

        Matcher volumeBetweenMatcher = VOLUME_BETWEEN_TIMES_PATTERN.matcher(normalized.toUpperCase(Locale.ROOT));
        if (volumeBetweenMatcher.find()) {
            plan.setAction(AiQueryAction.VOLUME_QUERY);
            plan.setSymbol(normalizeSymbol(volumeBetweenMatcher.group(1)));
            plan.setTime(normalizeTimeToken(volumeBetweenMatcher.group(2)));
            plan.setComparisonTime(normalizeTimeToken(volumeBetweenMatcher.group(3)));
            return plan;
        }

        Matcher topVolumeMatcher = TOP_VOLUME_PATTERN.matcher(normalized);
        if (topVolumeMatcher.find()) {
            plan.setAction(AiQueryAction.VOLUME_QUERY);
            plan.setDirection("top");
            plan.setSector(normalizeSector(topVolumeMatcher.group(2)));
            return plan;
        }

        if (plan.getSymbol() == null) {
            Matcher symbolMatcher = SYMBOL_AFTER_OF_PATTERN.matcher(normalized.toUpperCase(Locale.ROOT));
            if (symbolMatcher.find()) {
                plan.setSymbol(normalizeSymbol(symbolMatcher.group(1)));
            }
        }

        Matcher timeMatcher = TIME_PATTERN.matcher(normalized);
        String firstTime = null;
        String secondTime = null;
        if (timeMatcher.find()) {
            firstTime = normalizeTimeToken(timeMatcher.group());
        }
        if (timeMatcher.find()) {
            secondTime = normalizeTimeToken(timeMatcher.group());
        }

        if (firstTime != null && secondTime != null) {
            plan.setAction(AiQueryAction.COMPARE_PRICES);
            plan.setTime(firstTime);
            plan.setComparisonTime(secondTime);
            return plan;
        }

        if (firstTime != null) {
            plan.setAction(AiQueryAction.PRICE_AT_TIME);
            plan.setTime(firstTime);
            return plan;
        }

        plan.setAction(AiQueryAction.SNAPSHOT);
        return plan;
    }

    private void normalizePlan(AiQueryPlan plan, String requestedSymbol) {
        plan.setAction(plan.getAction() == null ? AiQueryAction.SNAPSHOT : plan.getAction());
        if (plan.getSymbol() == null) {
            plan.setSymbol(requestedSymbol);
        }
        plan.setSymbol(normalizeSymbol(plan.getSymbol()));
        plan.setComparisonSymbol(normalizeSymbol(plan.getComparisonSymbol()));
        plan.setTime(normalizeTimeToken(plan.getTime()));
        plan.setComparisonTime(normalizeTimeToken(plan.getComparisonTime()));
        plan.setComparisonSymbolTime(normalizeTimeToken(plan.getComparisonSymbolTime()));
        plan.setSector(normalizeSector(plan.getSector()));
        plan.setSymbols(normalizeSymbols(plan.getSymbols()));
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTimeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        Matcher matcher = TIME_PATTERN.matcher(value);
        if (!matcher.find()) {
            return value.matches("\\d{2}:\\d{2}") ? value : null;
        }

        int hour = Integer.parseInt(matcher.group(1));
        int minute = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        String meridiem = matcher.group(3).toLowerCase(Locale.ROOT);

        if ("pm".equals(meridiem) && hour < 12) {
            hour += 12;
        }
        if ("am".equals(meridiem) && hour == 12) {
            hour = 0;
        }

        return "%02d:%02d".formatted(hour, minute);
    }

    private String normalizeSector(String sector) {
        if (sector == null || sector.isBlank()) {
            return null;
        }
        return sector.trim().replaceAll("\\s+", " ");
    }

    private List<String> normalizeSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return symbols;
        }
        return symbols.stream()
                .map(this::normalizeSymbol)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> extractSymbols(String query) {
        Matcher matcher = SYMBOL_TOKEN_PATTERN.matcher(query);
        LinkedHashSet<String> symbols = new LinkedHashSet<>();
        while (matcher.find()) {
            String token = matcher.group();
            if (!Set.of("COMPARE", "PRICE", "AND", "WITH", "AFTER", "SECTOR", "VOLUME").contains(token)) {
                symbols.add(token);
            }
        }
        return new ArrayList<>(symbols);
    }

    private String stripCodeFences(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("```json", "").replace("```", "").trim();
    }
}
