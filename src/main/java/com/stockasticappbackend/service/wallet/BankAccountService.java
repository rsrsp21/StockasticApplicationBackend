package com.stockasticappbackend.service.wallet;

import java.util.List;

import com.stockasticappbackend.dto.wallet.BankAccountResponse;
import com.stockasticappbackend.dto.wallet.LinkBankAccountRequest;

/**
 * Service interface for bank account operations.
 */
public interface BankAccountService {

    /**
     * Links a new bank account to the user's profile.
     *
     * @param email   The user's email.
     * @param request The bank account details.
     * @return BankAccountResponse with the linked account (masked).
     */
    BankAccountResponse linkBankAccount(String email, LinkBankAccountRequest request);

    /**
     * Gets all bank accounts linked to the user.
     *
     * @param email The user's email.
     * @return List of BankAccountResponse with masked account numbers.
     */
    List<BankAccountResponse> getBankAccounts(String email);

    /**
     * Gets the primary (first) bank account for the user.
     *
     * @param email The user's email.
     * @return BankAccountResponse of the primary account, or null if none.
     */
    BankAccountResponse getPrimaryBankAccount(String email);

    /**
     * Deletes a bank account.
     *
     * @param email         The user's email.
     * @param bankAccountId The bank account ID to delete.
     */
    void deleteBankAccount(String email, Long bankAccountId);

    /**
     * Sets a bank account as the primary account for the user.
     *
     * @param email         The user's email.
     * @param bankAccountId The bank account ID to set as primary.
     * @return BankAccountResponse of the updated primary account.
     */
    BankAccountResponse setPrimaryBankAccount(String email, Long bankAccountId);
}
