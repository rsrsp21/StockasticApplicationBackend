package com.stockasticappbackend.service.pricealert;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PriceAlertResponse;
import com.stockasticappbackend.model.enums.PriceAlertCondition;

public interface PriceAlertService {

    boolean hasActiveAlerts(Long stockId);

    Set<Long> getStocksWithActiveAlerts();

    PriceAlertResponse createAlert(String email, Long stockId, BigDecimal targetPrice, PriceAlertCondition condition);

    List<PriceAlertResponse> getUserAlerts(String email);

    PageResponse<PriceAlertResponse> getPagedUserAlerts(String email, int page, int size, String sortBy,
            String sortDir, Boolean isActive);

    void deleteAlert(String email, Long alertId);

    void checkAndTriggerAlerts(Long stockId, BigDecimal currentPrice);
}
