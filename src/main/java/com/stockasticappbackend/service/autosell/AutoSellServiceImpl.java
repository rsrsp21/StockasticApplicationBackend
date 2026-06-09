package com.stockasticappbackend.service.autosell;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.github.benmanes.caffeine.cache.Cache;
import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.order.AutoSellRuleResponse;
import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.event.AutoSellRuleCreatedEvent;
import com.stockasticappbackend.event.HoldingsReducedEvent;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.AutoSellRule;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.NotificationType;
import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderType;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.holdings.HoldingsService;
import com.stockasticappbackend.service.notification.NotificationService;
import com.stockasticappbackend.service.order.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoSellServiceImpl implements AutoSellService {

    private final Cache<Long, List<AutoSellRule>> autoSellRuleCache;
    private final AutoSellRuleRepository autoSellRuleRepository;
    private final NotificationService notificationService;
    private final OrderService orderService;
    private final HoldingsService holdingsService;
    private final StockRepository stockRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Initializing AutoSellRule cache...");
        try {
            List<AutoSellRule> allActive = autoSellRuleRepository.findByIsActiveTrue();
            Map<Long, List<AutoSellRule>> grouped = allActive.stream()
                    .collect(Collectors.groupingBy(a -> a.getStock().getStockId()));

            autoSellRuleCache.invalidateAll();
            grouped.forEach(autoSellRuleCache::put);
            log.info("Loaded {} active auto-sell rules into cache", allActive.size());
        } catch (Exception e) {
            log.warn("Skipping AutoSellRule cache warmup during startup: {}", e.getMessage());
            autoSellRuleCache.invalidateAll();
        }
    }

    @Override
    public boolean hasActiveRules(Long stockId) {
        List<AutoSellRule> rules = autoSellRuleCache.getIfPresent(stockId);
        return rules != null && !rules.isEmpty();
    }

    @Override
    public Set<Long> getStocksWithActiveRules() {
        return autoSellRuleCache.asMap().keySet();
    }

    @Override
    @Transactional
    public AutoSellRuleResponse createRule(String email, Long stockId, BigDecimal targetPrice, BigDecimal stopLoss,
            Integer quantity) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        try {
            var holdings = holdingsService.getHoldings(email);
            var holding = holdings.stream()
                    .filter(h -> h.getStockId().equals(stockId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("You do not own this stock"));

            if (quantity != null && quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }

            if (quantity != null && holding.getQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient holdings. You own " + holding.getQuantity() + " shares.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (autoSellRuleRepository.existsByUserAndStockAndIsActiveTrue(user, stock)) {
            throw new IllegalStateException("You already have an active auto-sell rule for this stock");
        }

        AutoSellRule rule = AutoSellRule.builder()
                .user(user)
                .stock(stock)
                .targetPrice(targetPrice)
                .stopLoss(stopLoss)
                .quantity(quantity)
                .isActive(true)
                .build();

        log.info("Created Auto-Sell rule for user {} on {}", email, stock.getSymbol());
        AutoSellRule saved = autoSellRuleRepository.save(rule);

        List<AutoSellRule> cached = autoSellRuleCache.getIfPresent(stockId);
        if (cached == null) {
            cached = new ArrayList<>();
        }
        cached.add(saved);
        autoSellRuleCache.put(stockId, cached);

        eventPublisher.publishEvent(new AutoSellRuleCreatedEvent(this, stockId));

        return mapToResponse(saved);
    }

    @Override
    public List<AutoSellRuleResponse> getUserRules(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return autoSellRuleRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<AutoSellRuleResponse> getPagedUserRules(String email, int page, int size, String sortBy,
            String sortDir, Boolean isActive) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AutoSellRule> rulePage;
        if (isActive != null) {
            rulePage = autoSellRuleRepository.findByUserAndIsActive(user, isActive, pageable);
        } else {
            rulePage = autoSellRuleRepository.findByUser(user, pageable);
        }

        List<AutoSellRuleResponse> content = rulePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<AutoSellRuleResponse>builder()
                .content(content)
                .page(rulePage.getNumber())
                .size(rulePage.getSize())
                .totalElements(rulePage.getTotalElements())
                .totalPages(rulePage.getTotalPages())
                .first(rulePage.isFirst())
                .last(rulePage.isLast())
                .hasNext(rulePage.hasNext())
                .hasPrevious(rulePage.hasPrevious())
                .build();
    }

    private AutoSellRuleResponse mapToResponse(AutoSellRule rule) {
        return AutoSellRuleResponse.builder()
                .ruleId(rule.getRuleId())
                .stockId(rule.getStock().getStockId())
                .symbol(rule.getStock().getSymbol())
                .stockName(rule.getStock().getName())
                .targetPrice(rule.getTargetPrice())
                .stopLoss(rule.getStopLoss())
                .quantity(rule.getQuantity())
                .isActive(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .triggeredAt(rule.getTriggeredAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteRule(String email, Long ruleId) {
        AutoSellRule rule = autoSellRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found"));

        if (!rule.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Rule does not belong to user");
        }

        autoSellRuleRepository.delete(rule);

        List<AutoSellRule> cached = autoSellRuleCache.getIfPresent(rule.getStock().getStockId());
        if (cached != null) {
            cached.removeIf(r -> r.getRuleId().equals(ruleId));
            autoSellRuleCache.put(rule.getStock().getStockId(), cached);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivateInvalidRulesAfterHoldingsReduction(HoldingsReducedEvent event) {
        AppUser user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Stock stock = stockRepository.findById(event.getStockId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        List<AutoSellRule> activeRules = autoSellRuleRepository.findByUserAndStockAndIsActiveTrue(user, stock);
        if (activeRules.isEmpty()) {
            return;
        }

        int remainingQuantity = event.getRemainingQuantity();
        for (AutoSellRule rule : activeRules) {
            if (!shouldDeactivateRule(rule, remainingQuantity)) {
                continue;
            }

            rule.setActive(false);
            autoSellRuleRepository.save(rule);
            removeRuleFromCache(stock.getStockId(), rule.getRuleId());

            String message = buildInvalidRuleMessage(rule, remainingQuantity);
            notificationService.createNotification(
                    user,
                    stock.getStockId(),
                    "Auto-Sell Disabled: " + stock.getSymbol(),
                    message,
                    NotificationType.SYSTEM);

            log.info("Disabled auto-sell rule {} for user {} on {} because holdings dropped to {}",
                    rule.getRuleId(), user.getEmail(), stock.getSymbol(), remainingQuantity);
        }
    }

    @Override
    @Transactional
    public void checkAndTriggerRules(Long stockId, BigDecimal currentPrice) {
        List<AutoSellRule> activeRules = autoSellRuleCache.getIfPresent(stockId);

        if (activeRules == null || activeRules.isEmpty()) {
            return;
        }

        List<AutoSellRule> rulesToCheck = new ArrayList<>(activeRules);

        for (AutoSellRule rule : rulesToCheck) {
            boolean triggered = false;
            String reason = "";

            if (rule.getTargetPrice() != null && currentPrice.compareTo(rule.getTargetPrice()) >= 0) {
                triggered = true;
                reason = "Target Price Hit (Take Profit)";
            } else if (rule.getStopLoss() != null && currentPrice.compareTo(rule.getStopLoss()) <= 0) {
                triggered = true;
                reason = "Stop Loss Hit";
            }

            if (triggered) {
                activeRules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
                autoSellRuleCache.put(stockId, activeRules);
                processTriggeredRule(rule, stockId, currentPrice, reason);
            }
        }
    }

    private boolean shouldDeactivateRule(AutoSellRule rule, int remainingQuantity) {
        if (!rule.isActive()) {
            return false;
        }

        if (remainingQuantity <= 0) {
            return true;
        }

        return rule.getQuantity() != null && rule.getQuantity() > remainingQuantity;
    }

    private String buildInvalidRuleMessage(AutoSellRule rule, int remainingQuantity) {
        String symbol = rule.getStock().getSymbol();

        if (remainingQuantity <= 0) {
            return String.format(
                    "Your auto-sell rule for %s was disabled because you no longer hold any shares of this stock.",
                    symbol);
        }

        return String.format(
                "Your auto-sell rule for %d share(s) of %s was disabled because your holdings are now %d share(s).",
                rule.getQuantity(),
                symbol,
                remainingQuantity);
    }

    private void removeRuleFromCache(Long stockId, Long ruleId) {
        List<AutoSellRule> cached = autoSellRuleCache.getIfPresent(stockId);
        if (cached == null) {
            return;
        }

        cached.removeIf(rule -> rule.getRuleId().equals(ruleId));
        autoSellRuleCache.put(stockId, cached);
    }

    private void processTriggeredRule(AutoSellRule rule, Long stockId, BigDecimal currentPrice, String reason) {
        Stock stock = rule.getStock();
        log.info("Auto-Sell Rule Triggered & Removed from Cache! User: {}, Stock: {}, Reason: {}, Price: {}",
                rule.getUser().getEmail(), stock.getSymbol(), reason, currentPrice);

        try {
            var userHoldings = holdingsService.getHoldings(rule.getUser().getEmail());
            var holding = userHoldings.stream()
                    .filter(h -> h.getStockId().equals(stockId))
                    .findFirst();

            if (holding.isPresent() && holding.get().getQuantity() > 0) {
                Integer currentHoldingQty = holding.get().getQuantity();
                Integer qtyToSell = rule.getQuantity();

                if (qtyToSell == null || qtyToSell > currentHoldingQty) {
                    qtyToSell = currentHoldingQty;
                }

                OrderRequest request = new OrderRequest();
                request.setStockId(stockId);
                request.setOrderType(OrderType.SELL);
                request.setOrderMode(OrderMode.MARKET);
                request.setQuantity(qtyToSell);

                orderService.placeOrder(rule.getUser().getEmail(), request);

                String message = String.format("%s was sold at market price (%s) due to %s. Quantity: %d",
                        stock.getSymbol(), currentPrice, reason, qtyToSell);

                notificationService.createNotification(rule.getUser(),
                        stockId,
                        "Auto-Sell Executed: " + stock.getSymbol(), message, NotificationType.ORDER);

            } else {
                log.warn("Auto-Sell triggered but user has no holdings. Deactivating rule.");
                notificationService.createNotification(rule.getUser(),
                        stockId,
                        "Auto-Sell Failed: " + stock.getSymbol(),
                        "Auto-sell rule triggered but you have no shares to sell. Rule deactivated.",
                        NotificationType.SYSTEM);
            }

        } catch (Exception e) {
            log.error("Failed to execute auto-sell order", e);
            notificationService.createNotification(rule.getUser(),
                    stockId,
                    "Auto-Sell Error: " + stock.getSymbol(),
                    "Failed to execute auto-sell order: " + e.getMessage(),
                    NotificationType.SYSTEM);
        } finally {
            rule.setActive(false);
            rule.setTriggeredAt(LocalDateTime.now());
            autoSellRuleRepository.save(rule);
        }
    }
}
