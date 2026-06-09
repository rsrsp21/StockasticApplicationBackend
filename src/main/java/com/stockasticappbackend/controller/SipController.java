package com.stockasticappbackend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.sip.SipRequest;
import com.stockasticappbackend.dto.sip.SipResponse;
import com.stockasticappbackend.dto.sip.SipTransactionResponse;
import com.stockasticappbackend.model.enums.SipStatus;
import com.stockasticappbackend.service.sip.SipService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sip")
@RequiredArgsConstructor
public class SipController {

    private final SipService sipService;

    @PostMapping
    public ResponseEntity<SipResponse> createSip(
            @Valid @RequestBody SipRequest request,
            Principal principal) {
        return ResponseEntity.ok(sipService.createSip(principal.getName(), request));
    }

    @PutMapping("/{sipId}")
    public ResponseEntity<SipResponse> updateSip(
            @PathVariable Long sipId,
            @Valid @RequestBody SipRequest request,
            Principal principal) {
        return ResponseEntity.ok(sipService.updateSip(principal.getName(), sipId, request));
    }

    @PatchMapping("/{sipId}/status")
    public ResponseEntity<SipResponse> toggleSipStatus(
            @PathVariable Long sipId,
            @RequestParam SipStatus status,
            Principal principal) {
        return ResponseEntity.ok(sipService.toggleSipStatus(principal.getName(), sipId, status));
    }

    @GetMapping
    public ResponseEntity<List<SipResponse>> getUserSips(Principal principal) {
        return ResponseEntity.ok(sipService.getUserSips(principal.getName()));
    }

    @GetMapping("/stock/{stockId}")
    public ResponseEntity<List<SipResponse>> getSipsByStock(
            @PathVariable Long stockId,
            Principal principal) {
        return ResponseEntity.ok(sipService.getSipsByStock(principal.getName(), stockId));
    }

    @GetMapping("/{sipId}")
    public ResponseEntity<SipResponse> getSip(
            @PathVariable Long sipId,
            Principal principal) {
        return ResponseEntity.ok(sipService.getSip(principal.getName(), sipId));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<SipTransactionResponse>> getSipHistory(
            @PageableDefault(sort = "executionDate", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(sipService.getSipHistory(principal.getName(), pageable));
    }
}
