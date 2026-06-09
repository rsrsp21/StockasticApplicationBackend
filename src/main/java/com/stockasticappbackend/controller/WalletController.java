package com.stockasticappbackend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.wallet.AddFundsRequest;
import com.stockasticappbackend.dto.wallet.OtpResponse;
import com.stockasticappbackend.dto.wallet.SendOtpRequest;
import com.stockasticappbackend.dto.wallet.WalletResponse;
import com.stockasticappbackend.dto.wallet.WalletTransactionResponse;
import com.stockasticappbackend.dto.wallet.WithdrawFundsRequest;
import com.stockasticappbackend.service.wallet.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for wallet operations.
 * Provides endpoints for wallet management, fund operations, and transaction history.
 */
@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Gets the current user's wallet.
     * Creates a new wallet if one doesn't exist.
     *
     * @param authentication The authentication object.
     * @return WalletResponse containing wallet details.
     */
    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(Authentication authentication) {
        WalletResponse response = walletService.getOrCreateWallet(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Sends an OTP for wallet operations.
     *
     * @param authentication The authentication object.
     * @param request        The OTP request containing purpose and amount.
     * @return OtpResponse with status and expiry time.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<OtpResponse> sendOtp(
            Authentication authentication,
            @RequestBody @Valid SendOtpRequest request) {
        OtpResponse response = walletService.sendOtp(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Adds funds to the wallet after OTP verification.
     *
     * @param authentication The authentication object.
     * @param request        The add funds request.
     * @return WalletResponse with updated balance.
     */
    @PostMapping("/add-funds")
    public ResponseEntity<WalletResponse> addFunds(
            Authentication authentication,
            @RequestBody @Valid AddFundsRequest request) {
        WalletResponse response = walletService.addFunds(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraws funds from the wallet after OTP verification.
     *
     * @param authentication The authentication object.
     * @param request        The withdraw request.
     * @return WalletResponse with updated balance.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdrawFunds(
            Authentication authentication,
            @RequestBody @Valid WithdrawFundsRequest request) {
        WalletResponse response = walletService.withdrawFunds(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the transaction history for the current user's wallet.
     *
     * @param authentication The authentication object.
     * @param pageable       Pagination parameters.
     * @return Page of WalletTransactionResponse.
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactionHistory(
            Authentication authentication,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<WalletTransactionResponse> transactions = 
                walletService.getTransactionHistory(authentication.getName(), pageable);
        return ResponseEntity.ok(transactions);
    }
}
