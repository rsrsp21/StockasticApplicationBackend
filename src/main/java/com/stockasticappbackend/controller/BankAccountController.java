package com.stockasticappbackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockasticappbackend.dto.wallet.BankAccountResponse;
import com.stockasticappbackend.dto.wallet.LinkBankAccountRequest;
import com.stockasticappbackend.service.wallet.BankAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for bank account operations.
 * Provides endpoints for linking, listing, and deleting bank accounts.
 */
@RestController
@RequestMapping("/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /**
     * Links a new bank account to the user's profile.
     *
     * @param authentication The authentication object.
     * @param request        The bank account details.
     * @return BankAccountResponse with the linked account (masked).
     */
    @PostMapping("/link")
    public ResponseEntity<BankAccountResponse> linkBankAccount(
            Authentication authentication,
            @RequestBody @Valid LinkBankAccountRequest request) {
        BankAccountResponse response = bankAccountService.linkBankAccount(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all bank accounts linked to the user.
     *
     * @param authentication The authentication object.
     * @return List of BankAccountResponse with masked account numbers.
     */
    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> getBankAccounts(Authentication authentication) {
        List<BankAccountResponse> accounts = bankAccountService.getBankAccounts(authentication.getName());
        return ResponseEntity.ok(accounts);
    }

    /**
     * Gets the primary (first) bank account for the user.
     *
     * @param authentication The authentication object.
     * @return BankAccountResponse of the primary account.
     */
    @GetMapping("/primary")
    public ResponseEntity<BankAccountResponse> getPrimaryBankAccount(Authentication authentication) {
        BankAccountResponse account = bankAccountService.getPrimaryBankAccount(authentication.getName());
        if (account == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(account);
    }

    /**
     * Deletes a bank account.
     *
     * @param authentication The authentication object.
     * @param id             The bank account ID to delete.
     * @return ResponseEntity with no content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(
            Authentication authentication,
            @PathVariable Long id) {
        bankAccountService.deleteBankAccount(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sets a bank account as the primary account.
     *
     * @param authentication The authentication object.
     * @param id             The bank account ID to set as primary.
     * @return BankAccountResponse of the updated primary account.
     */
    @PutMapping("/{id}/primary")
    public ResponseEntity<BankAccountResponse> setPrimaryBankAccount(
            Authentication authentication,
            @PathVariable Long id) {
        BankAccountResponse account = bankAccountService.setPrimaryBankAccount(authentication.getName(), id);
        return ResponseEntity.ok(account);
    }
}
