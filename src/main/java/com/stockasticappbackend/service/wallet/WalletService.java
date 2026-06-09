package com.stockasticappbackend.service.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.stockasticappbackend.dto.wallet.AddFundsRequest;
import com.stockasticappbackend.dto.wallet.OtpResponse;
import com.stockasticappbackend.dto.wallet.SendOtpRequest;
import com.stockasticappbackend.dto.wallet.WalletResponse;
import com.stockasticappbackend.dto.wallet.WalletTransactionResponse;
import com.stockasticappbackend.dto.wallet.WithdrawFundsRequest;

/**
 * Service interface for wallet operations.
 */
public interface WalletService {

    /**
     * Gets the wallet for the authenticated user.
     * Creates a new wallet if one doesn't exist.
     *
     * @param email The user's email.
     * @return WalletResponse containing wallet details.
     */
    WalletResponse getOrCreateWallet(String email);

    /**
     * Sends an OTP for wallet operations (add funds, withdraw).
     *
     * @param email   The user's email.
     * @param request The OTP request containing purpose and amount.
     * @return OtpResponse with status and expiry time.
     */
    OtpResponse sendOtp(String email, SendOtpRequest request);

    /**
     * Adds funds to the user's wallet after OTP verification.
     *
     * @param email   The user's email.
     * @param request The add funds request containing amount, OTP, etc.
     * @return WalletResponse with updated balance.
     */
    WalletResponse addFunds(String email, AddFundsRequest request);

    /**
     * Withdraws funds from the user's wallet after OTP verification.
     *
     * @param email   The user's email.
     * @param request The withdraw request containing amount, bank account ID, OTP.
     * @return WalletResponse with updated balance.
     */
    WalletResponse withdrawFunds(String email, WithdrawFundsRequest request);

    /**
     * Gets paginated transaction history for the user's wallet.
     *
     * @param email    The user's email.
     * @param pageable Pagination parameters.
     * @return Page of WalletTransactionResponse.
     */
    Page<WalletTransactionResponse> getTransactionHistory(String email, Pageable pageable);
}
