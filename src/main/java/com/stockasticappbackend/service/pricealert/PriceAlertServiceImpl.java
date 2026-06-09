package com.stockasticappbackend.service.pricealert;

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
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PriceAlertResponse;
import com.stockasticappbackend.event.PriceAlertCreatedEvent;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.PriceAlert;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.NotificationType;
import com.stockasticappbackend.model.enums.PriceAlertCondition;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.PriceAlertRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertServiceImpl implements PriceAlertService {

    private final Cache<Long, List<PriceAlert>> priceAlertCache;
    private final PriceAlertRepository priceAlertRepository;
    private final NotificationService notificationService;
    private final StockRepository stockRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("Initializing PriceAlert cache...");
        try {
            List<PriceAlert> allActive = priceAlertRepository.findByIsActiveTrue();
            Map<Long, List<PriceAlert>> grouped = allActive.stream()
                    .collect(Collectors.groupingBy(a -> a.getStock().getStockId()));

            priceAlertCache.invalidateAll();
            grouped.forEach(priceAlertCache::put);
            log.info("Loaded {} active price alerts into cache", allActive.size());
        } catch (Exception e) {
            log.warn("Skipping PriceAlert cache warmup during startup: {}", e.getMessage());
            priceAlertCache.invalidateAll();
        }
    }

    @Override
    public boolean hasActiveAlerts(Long stockId) {
        List<PriceAlert> alerts = priceAlertCache.getIfPresent(stockId);
        return alerts != null && !alerts.isEmpty();
    }

    @Override
    public Set<Long> getStocksWithActiveAlerts() {
        return priceAlertCache.asMap().keySet();
    }

    @Override
    @Transactional
    public PriceAlertResponse createAlert(String email, Long stockId, BigDecimal targetPrice, PriceAlertCondition condition) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .stock(stock)
                .targetPrice(targetPrice)
                .condition(condition)
                .isActive(true)
                .build();

        log.info("Created price alert for user {} on {} at {}", email, stock.getSymbol(), targetPrice);
        PriceAlert saved = priceAlertRepository.save(alert);

        List<PriceAlert> cached = priceAlertCache.getIfPresent(stockId);
        if (cached == null) {
            cached = new ArrayList<>();
        }
        cached.add(saved);
        priceAlertCache.put(stockId, cached);

        eventPublisher.publishEvent(new PriceAlertCreatedEvent(this, stockId));

        return mapToResponse(saved);
    }

    @Override
    public List<PriceAlertResponse> getUserAlerts(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return priceAlertRepository.findByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PriceAlertResponse> getPagedUserAlerts(String email, int page, int size, String sortBy,
            String sortDir, Boolean isActive) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PriceAlert> alertPage;
        if (isActive != null) {
            alertPage = priceAlertRepository.findByUserAndIsActive(user, isActive, pageable);
        } else {
            alertPage = priceAlertRepository.findByUser(user, pageable);
        }

        List<PriceAlertResponse> content = alertPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<PriceAlertResponse>builder()
                .content(content)
                .page(alertPage.getNumber())
                .size(alertPage.getSize())
                .totalElements(alertPage.getTotalElements())
                .totalPages(alertPage.getTotalPages())
                .first(alertPage.isFirst())
                .last(alertPage.isLast())
                .hasNext(alertPage.hasNext())
                .hasPrevious(alertPage.hasPrevious())
                .build();
    }

    private PriceAlertResponse mapToResponse(PriceAlert alert) {
        return PriceAlertResponse.builder()
                .alertId(alert.getAlertId())
                .stockId(alert.getStock().getStockId())
                .symbol(alert.getStock().getSymbol())
                .stockName(alert.getStock().getName())
                .targetPrice(alert.getTargetPrice())
                .condition(alert.getCondition())
                .isActive(alert.isActive())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteAlert(String email, Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        if (!alert.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Alert does not belong to user");
        }

        priceAlertRepository.delete(alert);

        List<PriceAlert> cached = priceAlertCache.getIfPresent(alert.getStock().getStockId());
        if (cached != null) {
            cached.removeIf(a -> a.getAlertId().equals(alertId));
            priceAlertCache.put(alert.getStock().getStockId(), cached);
        }
    }

    @Override
    @Transactional
    public void checkAndTriggerAlerts(Long stockId, BigDecimal currentPrice) {
        List<PriceAlert> activeAlerts = priceAlertCache.getIfPresent(stockId);

        if (activeAlerts == null || activeAlerts.isEmpty()) {
            return;
        }

        List<PriceAlert> alertsToCheck = new ArrayList<>(activeAlerts);

        for (PriceAlert alert : alertsToCheck) {
            boolean triggered = false;

            if (alert.getCondition() == PriceAlertCondition.ABOVE && currentPrice.compareTo(alert.getTargetPrice()) >= 0) {
                triggered = true;
            } else if (alert.getCondition() == PriceAlertCondition.BELOW && currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                triggered = true;
            }

            if (triggered) {
                Stock stock = alert.getStock();
                log.info("Price Alert Triggered! User: {}, Stock: {}, Price: {}",
                        alert.getUser().getEmail(), stock.getSymbol(), currentPrice);

                String title = "Price Alert: " + stock.getSymbol();
                String message = String.format("%s has reached your target of %s (Current: %s)",
                        stock.getSymbol(), alert.getTargetPrice(), currentPrice);

                notificationService.createNotification(
                        alert.getUser(),
                        stock.getStockId(),
                        title,
                        message,
                        NotificationType.ALERT);

                alert.setActive(false);
                alert.setTriggeredAt(LocalDateTime.now());
                priceAlertRepository.save(alert);

                List<PriceAlert> cachedAlerts = priceAlertCache.getIfPresent(stockId);
                if (cachedAlerts != null) {
                    cachedAlerts.removeIf(a -> a.getAlertId().equals(alert.getAlertId()));
                    priceAlertCache.put(stockId, cachedAlerts);
                }
            }
        }
    }
}
