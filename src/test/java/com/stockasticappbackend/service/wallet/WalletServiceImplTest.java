package com.stockasticappbackend.service.wallet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.stockasticappbackend.dto.wallet.AddFundsRequest;
import com.stockasticappbackend.dto.wallet.LinkBankAccountRequest;
import com.stockasticappbackend.dto.wallet.OtpResponse;
import com.stockasticappbackend.dto.wallet.SendOtpRequest;
import com.stockasticappbackend.dto.wallet.WalletResponse;
import com.stockasticappbackend.dto.wallet.WalletTransactionResponse;
import com.stockasticappbackend.dto.wallet.WithdrawFundsRequest;
import com.stockasticappbackend.exception.InsufficientFundsException;
import com.stockasticappbackend.exception.InvalidOtpException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.exception.WalletNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.BankAccountRepository;
import com.stockasticappbackend.repository.HoldingsRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.PriceAlertRepository;
import com.stockasticappbackend.repository.SipRepository;
import com.stockasticappbackend.repository.StockIndicatorRepository;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WalletRepository;
import com.stockasticappbackend.repository.WalletTransactionRepository;
import com.stockasticappbackend.repository.WatchlistItemRepository;
import com.stockasticappbackend.repository.WatchlistRepository;

@SpringBootTest
class WalletServiceImplTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private KycRepository kycRepository;

    private AppUser testUser;
    
    @BeforeEach
    void setUp() {
        stockIndicatorRepository.deleteAll();
        watchlistItemRepository.deleteAll();
        watchlistRepository.deleteAll();
        priceAlertRepository.deleteAll();
        sipRepository.deleteAll();
        autoSellRuleRepository.deleteAll();
        transactionRepository.deleteAll();
        orderRepository.deleteAll();
        holdingsRepository.deleteAll();
        stockPriceRepository.deleteAll();
        kycRepository.deleteAll();
        walletRepository.deleteAll();
        bankAccountRepository.deleteAll();
        stockRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setEmail("wallet@example.com");
        testUser.setName("Wallet User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void getOrCreateWallet_NewUser_ShouldCreateWallet() {
        WalletResponse response = walletService.getOrCreateWallet("wallet@example.com");
        assertNotNull(response);
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getAvailableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getLockedBalance()));
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void getOrCreateWallet_ExistingWallet_ShouldReturnSame() {
        WalletResponse first = walletService.getOrCreateWallet("wallet@example.com");
        WalletResponse second = walletService.getOrCreateWallet("wallet@example.com");
        assertEquals(first.getWalletId(), second.getWalletId());
    }

    @Test
    void sendOtp_ShouldReturnSuccess() {
        SendOtpRequest request = new SendOtpRequest();
        request.setPurpose("ADD_FUNDS");
        OtpResponse response = walletService.sendOtp("wallet@example.com", request);
        assertTrue(response.isSuccess());
        assertEquals(300, response.getExpirySeconds());
    }

    @Test
    void sendOtp_UserNotFound_ShouldThrowException() {
        SendOtpRequest request = new SendOtpRequest();
        request.setPurpose("ADD_FUNDS");
        assertThrows(ResourceNotFoundException.class, () -> {
            walletService.sendOtp("nonexistent@example.com", request);
        });
    }

    @Test
    void addFunds_ValidOtp_ShouldIncreaseBalance() {
        walletService.getOrCreateWallet("wallet@example.com");
        String otp = otpService.generateOtp("wallet@example.com", "ADD_FUNDS");

        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMethod("UPI");
        request.setOtp(otp);
        WalletResponse response = walletService.addFunds("wallet@example.com", request);
        assertEquals(0, new BigDecimal("5000.00").compareTo(response.getAvailableBalance()));
    }

    @Test
    void addFunds_InvalidOtp_ShouldThrowException() {
        walletService.getOrCreateWallet("wallet@example.com");

        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMethod("UPI");
        request.setOtp("0000");
        assertThrows(InvalidOtpException.class, () -> {
            walletService.addFunds("wallet@example.com", request);
        });
    }

    @Test
    void addFunds_ZeroAmount_ShouldThrowException() {
        walletService.getOrCreateWallet("wallet@example.com");
        String otp = otpService.generateOtp("wallet@example.com", "ADD_FUNDS");

        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(BigDecimal.ZERO);
        request.setPaymentMethod("UPI");
        request.setOtp(otp);
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.addFunds("wallet@example.com", request);
        });
    }

    @Test
    void addFunds_MultipleTimes_ShouldAccumulateBalance() {
        walletService.getOrCreateWallet("wallet@example.com");
        String otp1 = otpService.generateOtp("wallet@example.com", "ADD_FUNDS");
        AddFundsRequest r1 = new AddFundsRequest();
        r1.setAmount(new BigDecimal("3000.00"));
        r1.setPaymentMethod("UPI");
        r1.setOtp(otp1);
        walletService.addFunds("wallet@example.com", r1);
        String otp2 = otpService.generateOtp("wallet@example.com", "ADD_FUNDS");
        AddFundsRequest r2 = new AddFundsRequest();
        r2.setAmount(new BigDecimal("2000.00"));
        r2.setPaymentMethod("CARD");
        r2.setOtp(otp2);
        WalletResponse response = walletService.addFunds("wallet@example.com", r2);
        assertEquals(0, new BigDecimal("5000.00").compareTo(response.getAvailableBalance()));
    }

    @Test
    void withdrawFunds_SufficientBalance_ShouldDecreaseBalance() {
        walletService.getOrCreateWallet("wallet@example.com");
        String addOtp = otpService.generateOtp("wallet@example.com", "ADD_FUNDS");
        AddFundsRequest addRequest = new AddFundsRequest();
        addRequest.setAmount(new BigDecimal("10000.00"));
        addRequest.setPaymentMethod("UPI");
        addRequest.setOtp(addOtp);
        walletService.addFunds("wallet@example.com", addRequest);
        LinkBankAccountRequest bankReq = new LinkBankAccountRequest();
        bankReq.setBankName("HDFC Bank");
        bankReq.setAccountNumber("123456789012");
        bankReq.setIfscCode("HDFC0001234");
        bankReq.setHolderName("Wallet User");
        var bankAccount = bankAccountService.linkBankAccount("wallet@example.com", bankReq);
        String withdrawOtp = otpService.generateOtp("wallet@example.com", "WITHDRAW");
        WithdrawFundsRequest withdrawRequest = new WithdrawFundsRequest();
        withdrawRequest.setAmount(new BigDecimal("3000.00"));
        withdrawRequest.setBankAccountId(bankAccount.getId());
        withdrawRequest.setOtp(withdrawOtp);
        WalletResponse response = walletService.withdrawFunds("wallet@example.com", withdrawRequest);
        assertEquals(0, new BigDecimal("7000.00").compareTo(response.getAvailableBalance()));
    }

    @Test
    void withdrawFunds_InsufficientBalance_ShouldThrowException() {
        walletService.getOrCreateWallet("wallet@example.com");
        LinkBankAccountRequest bankReq = new LinkBankAccountRequest();
        bankReq.setBankName("HDFC Bank");
        bankReq.setAccountNumber("123456789012");
        bankReq.setIfscCode("HDFC0001234");
        bankReq.setHolderName("Wallet User");
        var bankAccount = bankAccountService.linkBankAccount("wallet@example.com", bankReq);

        String withdrawOtp = otpService.generateOtp("wallet@example.com", "WITHDRAW");
        WithdrawFundsRequest withdrawRequest = new WithdrawFundsRequest();
        withdrawRequest.setAmount(new BigDecimal("5000.00"));
        withdrawRequest.setBankAccountId(bankAccount.getId());
        withdrawRequest.setOtp(withdrawOtp);
        assertThrows(InsufficientFundsException.class, () -> {
            walletService.withdrawFunds("wallet@example.com", withdrawRequest);
        });
    }

    @Test
    void withdrawFunds_InvalidOtp_ShouldThrowException() {
        walletService.getOrCreateWallet("wallet@example.com");

        WithdrawFundsRequest request = new WithdrawFundsRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setBankAccountId(1L);
        request.setOtp("0000");
        assertThrows(InvalidOtpException.class, () -> {
            walletService.withdrawFunds("wallet@example.com", request);
        });
    }

    @Test
    void getTransactionHistory_ShouldReturnTransactions() {
        walletService.getOrCreateWallet("wallet@example.com");
        String otp = otpService.generateOtp("wallet@example.com", "ADD_FUNDS");
        AddFundsRequest request = new AddFundsRequest();
        request.setAmount(new BigDecimal("5000.00"));
        request.setPaymentMethod("UPI");
        request.setOtp(otp);
        walletService.addFunds("wallet@example.com", request);
        Page<WalletTransactionResponse> history = walletService.getTransactionHistory(
                "wallet@example.com", PageRequest.of(0, 10));
        assertNotNull(history);
        assertEquals(1, history.getTotalElements());
        assertEquals(0, new BigDecimal("5000.00").compareTo(history.getContent().get(0).getAmount()));
    }

    @Test
    void getTransactionHistory_NoWallet_ShouldThrowException() {
        assertThrows(WalletNotFoundException.class, () -> {
            walletService.getTransactionHistory("wallet@example.com", PageRequest.of(0, 10));
        });
    }
}

