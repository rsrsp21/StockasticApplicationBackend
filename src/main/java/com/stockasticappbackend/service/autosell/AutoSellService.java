package com.stockasticappbackend.service.autosell;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.order.AutoSellRuleResponse;

public interface AutoSellService {

    boolean hasActiveRules(Long stockId);

    Set<Long> getStocksWithActiveRules();

    AutoSellRuleResponse createRule(String email, Long stockId, BigDecimal targetPrice, BigDecimal stopLoss,
            Integer quantity);

    List<AutoSellRuleResponse> getUserRules(String email);

    PageResponse<AutoSellRuleResponse> getPagedUserRules(String email, int page, int size, String sortBy,
            String sortDir, Boolean isActive);

    void deleteRule(String email, Long ruleId);

    void checkAndTriggerRules(Long stockId, BigDecimal currentPrice);
}
