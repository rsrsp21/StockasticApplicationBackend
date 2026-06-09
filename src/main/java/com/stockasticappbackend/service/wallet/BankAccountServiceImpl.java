package com.stockasticappbackend.service.wallet;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.wallet.BankAccountResponse;
import com.stockasticappbackend.dto.wallet.LinkBankAccountRequest;
import com.stockasticappbackend.exception.BankAccountNotFoundException;
import com.stockasticappbackend.exception.DuplicateResourceException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.BankAccountMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.BankAccount;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.BankAccountRepository;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of BankAccountService.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final AppUserRepository appUserRepository;
    private final BankAccountMapper bankAccountMapper;

    @Override
    public BankAccountResponse linkBankAccount(String email, LinkBankAccountRequest request) {
        AppUser user = findUser(email);

        // Check if account already linked
        if (bankAccountRepository.existsByUserAndAccountNumber(user, request.getAccountNumber())) {
            throw new DuplicateResourceException(Constants.BANK_ACCOUNT_ALREADY_LINKED);
        }

        // Check if this is the first account (to make it primary)
        List<BankAccount> existingAccounts = bankAccountRepository.findByUser(user);
        boolean isFirstAccount = existingAccounts.isEmpty();

        BankAccount bankAccount = bankAccountMapper.toEntity(request);
        bankAccount.setUser(user);
        bankAccount.setIsVerified(true); // Auto-verify for demo
        bankAccount.setIsPrimary(isFirstAccount); // First account is primary

        BankAccount saved = bankAccountRepository.save(bankAccount);
        return bankAccountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponse> getBankAccounts(String email) {
        AppUser user = findUser(email);
        List<BankAccount> accounts = bankAccountRepository.findByUser(user);
        return bankAccountMapper.toResponseList(accounts);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountResponse getPrimaryBankAccount(String email) {
        AppUser user = findUser(email);
        // First try to find the explicitly marked primary account
        return bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .or(() -> bankAccountRepository.findFirstByUser(user)) // Fallback to first account
                .map(bankAccountMapper::toResponse)
                .orElse(null);
    }

    @Override
    public void deleteBankAccount(String email, Long bankAccountId) {
        AppUser user = findUser(email);
        
        BankAccount bankAccount = bankAccountRepository.findByIdAndUser(bankAccountId, user)
                .orElseThrow(() -> new BankAccountNotFoundException(Constants.BANK_ACCOUNT_NOT_FOUND));

        boolean wasPrimary = bankAccount.getIsPrimary();
        bankAccountRepository.delete(bankAccount);

        // If deleted account was primary, set another account as primary
        if (wasPrimary) {
            bankAccountRepository.findFirstByUser(user)
                    .ifPresent(account -> {
                        account.setIsPrimary(true);
                        bankAccountRepository.save(account);
                    });
        }
    }

    @Override
    public BankAccountResponse setPrimaryBankAccount(String email, Long bankAccountId) {
        AppUser user = findUser(email);
        
        // Find the account to set as primary
        BankAccount newPrimary = bankAccountRepository.findByIdAndUser(bankAccountId, user)
                .orElseThrow(() -> new BankAccountNotFoundException(Constants.BANK_ACCOUNT_NOT_FOUND));

        // Un-set current primary account if exists
        bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .ifPresent(currentPrimary -> {
                    if (!currentPrimary.getId().equals(bankAccountId)) {
                        currentPrimary.setIsPrimary(false);
                        bankAccountRepository.save(currentPrimary);
                    }
                });

        // Set the new account as primary
        newPrimary.setIsPrimary(true);
        BankAccount saved = bankAccountRepository.save(newPrimary);

        return bankAccountMapper.toResponse(saved);
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
