package com.stockasticappbackend.service.wallet;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.wallet.AddFundsRequest;
import com.stockasticappbackend.dto.wallet.OtpResponse;
import com.stockasticappbackend.dto.wallet.SendOtpRequest;
import com.stockasticappbackend.dto.wallet.WalletResponse;
import com.stockasticappbackend.dto.wallet.WalletTransactionResponse;
import com.stockasticappbackend.dto.wallet.WithdrawFundsRequest;
import com.stockasticappbackend.exception.BankAccountNotFoundException;
import com.stockasticappbackend.exception.InsufficientFundsException;
import com.stockasticappbackend.exception.InvalidOtpException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.exception.WalletNotFoundException;
import com.stockasticappbackend.mapper.WalletMapper;
import com.stockasticappbackend.mapper.WalletTransactionMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.BankAccount;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.entity.WalletTransaction;
import com.stockasticappbackend.model.enums.TransactionStatus;
import com.stockasticappbackend.model.enums.TransactionType;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.BankAccountRepository;
import com.stockasticappbackend.repository.WalletRepository;
import com.stockasticappbackend.repository.WalletTransactionRepository;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of WalletService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AppUserRepository appUserRepository;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper transactionMapper;
    private final OtpService otpService;

    private static final int OTP_EXPIRY_SECONDS = 300;

    @Override
    public WalletResponse getOrCreateWallet(String email) {
        AppUser user = findUser(email);
        
        Wallet wallet = walletRepository.findByUser(user)
                .orElseGet(() -> createWallet(user));

        return walletMapper.toResponse(wallet);
    }

    @Override
    public OtpResponse sendOtp(String email, SendOtpRequest request) {
        findUser(email);
        otpService.generateOtp(email, request.getPurpose());

        return OtpResponse.builder()
                .success(true)
                .message(Constants.OTP_SENT_SUCCESS)
                .expirySeconds(OTP_EXPIRY_SECONDS)
                .build();
    }

    @Override
    public WalletResponse addFunds(String email, AddFundsRequest request) {
        if (!otpService.verifyOtp(email, request.getOtp(), "ADD_FUNDS")) {
            throw new InvalidOtpException(Constants.OTP_INVALID);
        }

        AppUser user = findUser(email);
        Wallet wallet = getOrCreateWalletEntity(user);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(Constants.INVALID_DEPOSIT_AMOUNT);
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        createTransaction(
                wallet,
                request.getAmount(),
                TransactionType.CREDIT,
                TransactionStatus.SUCCESS,
                generateReferenceId("DEP"),
                request.getDescription() != null ? request.getDescription() : "Added via " + request.getPaymentMethod()
        );

        otpService.invalidateOtp(email, "ADD_FUNDS");

        return walletMapper.toResponse(wallet);
    }

    @Override
    public WalletResponse withdrawFunds(String email, WithdrawFundsRequest request) {
        if (!otpService.verifyOtp(email, request.getOtp(), "WITHDRAW")) {
            throw new InvalidOtpException(Constants.OTP_INVALID);
        }

        AppUser user = findUser(email);
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new WalletNotFoundException(Constants.WALLET_NOT_FOUND));

        BankAccount bankAccount = bankAccountRepository.findByIdAndUser(request.getBankAccountId(), user)
                .orElseThrow(() -> new BankAccountNotFoundException(Constants.BANK_ACCOUNT_NOT_FOUND));

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(Constants.INVALID_WITHDRAWAL_AMOUNT);
        }

        if (wallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(Constants.INSUFFICIENT_FUNDS);
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        createTransaction(
                wallet,
                request.getAmount(),
                TransactionType.DEBIT,
                TransactionStatus.SUCCESS,
                generateReferenceId("WTH"),
                "Withdrawal to " + bankAccount.getBankName()
        );

        otpService.invalidateOtp(email, "WITHDRAW");

        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionHistory(String email, Pageable pageable) {
        AppUser user = findUser(email);
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new WalletNotFoundException(Constants.WALLET_NOT_FOUND));

        Page<WalletTransaction> transactions = transactionRepository
                .findByWalletWalletIdOrderByCreatedAtDesc(wallet.getWalletId(), pageable);

        return transactions.map(transactionMapper::toResponse);
    }

    /**
     * Creates a new wallet for a user.
     *
     * @param user The user entity.
     * @return The created wallet.
     */
    private Wallet createWallet(AppUser user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        return walletRepository.save(wallet);
    }

    /**
     * Gets or creates a wallet entity for a user.
     *
     * @param user The user entity.
     * @return The wallet entity.
     */
    private Wallet getOrCreateWalletEntity(AppUser user) {
        return walletRepository.findByUser(user)
                .orElseGet(() -> createWallet(user));
    }

    /**
     * Creates a transaction record.
     *
     * @param wallet      The wallet entity.
     * @param amount      The transaction amount.
     * @param type        The transaction type.
     * @param status      The transaction status.
     * @param referenceId The external reference ID.
     * @param description The transaction description.
     */
    private void createTransaction(Wallet wallet, BigDecimal amount, TransactionType type,
                                   TransactionStatus status, String referenceId, String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setReferenceId(referenceId);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }

    /**
     * Generates a unique reference ID for transactions.
     *
     * @param prefix The prefix for the reference ID.
     * @return The generated reference ID.
     */
    private String generateReferenceId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Finds a user by email.
     *
     * @param email The user's email.
     * @return The AppUser entity.
     * @throws ResourceNotFoundException if user not found.
     */
    private AppUser findUser(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));
    }
}
