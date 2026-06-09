package com.stockasticappbackend.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.order.AutoSellRuleResponse;
import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.service.autosell.AutoSellService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/autosell")
@RequiredArgsConstructor
public class AutoSellController {

    private final AutoSellService autoSellService;

    @GetMapping
    public ResponseEntity<List<AutoSellRuleResponse>> getUserRules(Principal principal) {
        return ResponseEntity.ok(autoSellService.getUserRules(principal.getName()));
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<AutoSellRuleResponse>> getPagedUserRules(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(autoSellService.getPagedUserRules(principal.getName(), page, size, sortBy, sortDir, isActive));
    }

    @PostMapping
    public ResponseEntity<AutoSellRuleResponse> createRule(Principal principal, @RequestBody AutoSellRequest request) {
        return ResponseEntity.ok(autoSellService.createRule(
            principal.getName(), 
            request.getStockId(), 
            request.getTargetPrice(), 
            request.getStopLoss(),
            request.getQuantity()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(Principal principal, @PathVariable Long id) {
        autoSellService.deleteRule(principal.getName(), id);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class AutoSellRequest {
        private Long stockId;
        private BigDecimal targetPrice; // Profit Goal
        private BigDecimal stopLoss;    // Safety Net
        private Integer quantity;       // Shares to sell
    }
}
