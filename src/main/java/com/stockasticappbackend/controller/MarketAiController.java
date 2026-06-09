package com.stockasticappbackend.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.stockasticappbackend.dto.marketai.MarketAiQueryRequest;
import com.stockasticappbackend.dto.marketai.MarketAiQueryResponse;
import com.stockasticappbackend.service.marketai.MarketAiQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequestMapping("/ai/market")
@RequiredArgsConstructor
public class MarketAiController {

    private final MarketAiQueryService marketAiQueryService;

    @PostMapping("/query")
    public MarketAiQueryResponse query(@Valid @RequestBody MarketAiQueryRequest request) {
        try {
            return marketAiQueryService.answer(request);
        } catch (Exception ex) {
            return buildErrorResponse(request, ex);
        }
    }

    private MarketAiQueryResponse buildErrorResponse(MarketAiQueryRequest request, Throwable error) {
        String normalizedSymbol = request.getSymbol() == null ? null : request.getSymbol().trim().toUpperCase();

        if (error instanceof ResponseStatusException responseStatusException) {
            return MarketAiQueryResponse.builder()
                    .model("fallback")
                    .symbol(normalizedSymbol)
                    .answer(mapResponseStatusMessage(responseStatusException))
                    .snapshot(null)
                    .build();
        }

        return MarketAiQueryResponse.builder()
                .model("fallback")
                .symbol(normalizedSymbol)
                .answer(mapGenericErrorMessage(error))
                .snapshot(null)
                .build();
    }

    private String mapResponseStatusMessage(ResponseStatusException exception) {
        String reason = exception.getReason();
        String message = reason == null ? "" : reason.trim();

        if (message.contains("only supports stock-market questions")) {
            return "I can help only with stock-market questions. Try asking about a stock, price, comparison, candle, gainers, or losers.";
        }
        if (message.contains("educational analysis only")) {
            return "I can help with market insights and stock analysis, but not personalized financial advice. Try asking for a comparison, trend, price, movers, or sector view.";
        }
        if (message.startsWith("No stock found for symbol")) {
            return message + ". Please check the symbol and try again.";
        }
        if (message.startsWith("No latest price found for symbol")) {
            return message + ". I could not find recent market data for that stock.";
        }
        if (message.startsWith("No candle found for")) {
            return message + " Try another trading time or a different stock.";
        }
        if (message.contains("A stock symbol is required")) {
            return "Please include a stock symbol for that request, for example: compare TCS with INFY.";
        }
        if (message.contains("A valid HH:mm time is required")) {
            return "Please provide the time in a clear format like 10:00 am or 14:00.";
        }
        if (message.contains("No price history found")) {
            return "I could not find price history for that stock yet.";
        }

        return message.isBlank()
                ? "I could not process that market query right now. Please try again."
                : message;
    }

    private String mapGenericErrorMessage(Throwable error) {
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().trim();

        if (message.contains("Failed to generate content")) {
            return "I could not generate the AI narrative just now, but the market query itself is valid. Please retry in a moment.";
        }
        if (message.contains("429")) {
            return "The AI provider is rate-limiting requests right now. Please wait a bit and try again.";
        }
        if (message.contains("404") && message.toLowerCase().contains("model")) {
            return "The configured AI model is not available right now. Please check the provider model setting and try again.";
        }

        return "I could not process that market query right now. Please try again in a moment.";
    }
}
