package com.stockasticappbackend.controller;

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
import com.stockasticappbackend.dto.user.PriceAlertRequest;
import com.stockasticappbackend.dto.user.PriceAlertResponse;
import com.stockasticappbackend.service.pricealert.PriceAlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @GetMapping
    public ResponseEntity<List<PriceAlertResponse>> getUserAlerts(Principal principal) {
        return ResponseEntity.ok(priceAlertService.getUserAlerts(principal.getName()));
    }

    @GetMapping("/paged")
    public ResponseEntity<PageResponse<PriceAlertResponse>> getPagedUserAlerts(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(priceAlertService.getPagedUserAlerts(principal.getName(), page, size, sortBy, sortDir, isActive));
    }

    @PostMapping
    public ResponseEntity<PriceAlertResponse> createAlert(Principal principal, @RequestBody PriceAlertRequest request) {
        return ResponseEntity.ok(priceAlertService.createAlert(
            principal.getName(), 
            request.getStockId(), 
            request.getTargetPrice(), 
            request.getCondition()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(Principal principal, @PathVariable Long id) {
        priceAlertService.deleteAlert(principal.getName(), id);
        return ResponseEntity.ok().build();
    }
}
