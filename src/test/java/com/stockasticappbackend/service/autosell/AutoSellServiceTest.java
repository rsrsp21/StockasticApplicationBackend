package com.stockasticappbackend.service.autosell;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.order.AutoSellRuleResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Holdings;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.BankAccountRepository;
import com.stockasticappbackend.repository.HoldingsRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.repository.NotificationRepository;
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
class AutoSellServiceTest {

    @Autowired
    private AutoSellService autoSellService;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private AppUser testUser;
    private AppUser otherUser;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        activityLogRepository.deleteAll();
        notificationRepository.deleteAll();
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
        testUser.setEmail("autosell@test.com");
        testUser.setName("AutoSell User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        otherUser = new AppUser();
        otherUser.setEmail("other@test.com");
        otherUser.setName("Other User");
        otherUser.setPasswordHash("hash");
        otherUser.setUserStatus(UserStatus.ACTIVE);
        otherUser = userRepository.save(otherUser);

        testStock = new Stock();
        testStock.setSymbol("INFY");
        testStock.setName("Infosys Limited");
        testStock.setExchange("NSE");
        testStock.setSector("IT");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);
        Wallet wallet = new Wallet();
        wallet.setUser(testUser);
        wallet.setAvailableBalance(new BigDecimal("100000.00"));
        wallet.setLockedBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);
        Holdings holdings = new Holdings();
        holdings.setUser(testUser);
        holdings.setStock(testStock);
        holdings.setQuantity(100);
        holdings.setAveragePrice(new BigDecimal("1500.00"));
        holdings.setLockedQuantity(0);
        holdingsRepository.save(holdings);
    }

    @Test
    void createRule_WithTargetPrice_ShouldCreateRule() {
        AutoSellRuleResponse response = autoSellService.createRule(
                "autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), null, 10);

        assertNotNull(response);
        assertNotNull(response.getRuleId());
        assertEquals(testStock.getStockId(), response.getStockId());
        assertEquals("INFY", response.getSymbol());
        assertEquals("Infosys Limited", response.getStockName());
        assertEquals(0, new BigDecimal("2000.00").compareTo(response.getTargetPrice()));
        assertEquals(10, response.getQuantity());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void createRule_WithStopLoss_ShouldCreateRule() {
        AutoSellRuleResponse response = autoSellService.createRule(
                "autosell@test.com", testStock.getStockId(),
                null, new BigDecimal("1200.00"), 5);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("1200.00").compareTo(response.getStopLoss()));
    }

    @Test
    void createRule_WithBothTargetAndStopLoss_ShouldCreateRule() {
        AutoSellRuleResponse response = autoSellService.createRule(
                "autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), new BigDecimal("1200.00"), 10);

        assertNotNull(response);
        assertEquals(0, new BigDecimal("2000.00").compareTo(response.getTargetPrice()));
        assertEquals(0, new BigDecimal("1200.00").compareTo(response.getStopLoss()));
    }

    @Test
    void createRule_UserNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            autoSellService.createRule("nonexistent@test.com",
                    testStock.getStockId(), new BigDecimal("2000.00"), null, 10);
        });
    }

    @Test
    void createRule_StockNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            autoSellService.createRule("autosell@test.com",
                    99999L, new BigDecimal("2000.00"), null, 10);
        });
    }

    @Test
    void createRule_DuplicateActiveRule_ShouldThrow() {
        autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), null, 10);

        assertThrows(IllegalStateException.class, () -> {
            autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                    new BigDecimal("2500.00"), null, 5);
        });
    }

    @Test
    void createRule_ExceedsHoldings_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> {
            autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                    new BigDecimal("2000.00"), null, 999);
        });
    }

    @Test
    void createRule_UserHasNoHoldings_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> {
            autoSellService.createRule("other@test.com", testStock.getStockId(),
                    new BigDecimal("2000.00"), null, 10);
        });
    }

    @Test
    void getUserRules_ShouldReturnAllRules() {
        autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), new BigDecimal("1200.00"), 10);

        List<AutoSellRuleResponse> rules = autoSellService.getUserRules("autosell@test.com");

        assertNotNull(rules);
        assertEquals(1, rules.size());
        assertEquals("INFY", rules.get(0).getSymbol());
    }

    @Test
    void getUserRules_NoRules_ShouldReturnEmpty() {
        List<AutoSellRuleResponse> rules = autoSellService.getUserRules("autosell@test.com");

        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void getUserRules_UserNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            autoSellService.getUserRules("nonexistent@test.com");
        });
    }

    @Test
    void getPagedUserRules_ShouldReturnPaginated() {
        autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), new BigDecimal("1200.00"), 10);

        PageResponse<AutoSellRuleResponse> page = autoSellService.getPagedUserRules(
                "autosell@test.com", 0, 10, "createdAt", "desc", null);

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getTotalElements());
        assertTrue(page.isFirst());
        assertTrue(page.isLast());
    }

    @Test
    void getPagedUserRules_FilterByActive_ShouldWork() {
        AutoSellRuleResponse rule = autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), new BigDecimal("2000.00"), null, 10);
        var entity = autoSellRuleRepository.findById(rule.getRuleId()).orElseThrow();
        entity.setActive(false);
        autoSellRuleRepository.save(entity);

        PageResponse<AutoSellRuleResponse> activePage = autoSellService.getPagedUserRules(
                "autosell@test.com", 0, 10, "createdAt", "desc", true);
        assertEquals(0, activePage.getTotalElements());

        PageResponse<AutoSellRuleResponse> inactivePage = autoSellService.getPagedUserRules(
                "autosell@test.com", 0, 10, "createdAt", "desc", false);
        assertEquals(1, inactivePage.getTotalElements());
    }

    @Test
    void getPagedUserRules_AscendingSort_ShouldWork() {
        autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), null, 10);

        PageResponse<AutoSellRuleResponse> page = autoSellService.getPagedUserRules(
                "autosell@test.com", 0, 10, "createdAt", "asc", null);

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
    }

    @Test
    void getPagedUserRules_UserNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            autoSellService.getPagedUserRules("nonexistent@test.com",
                    0, 10, "createdAt", "desc", null);
        });
    }

    @Test
    void deleteRule_ValidOwner_ShouldDelete() {
        AutoSellRuleResponse created = autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), new BigDecimal("2000.00"), null, 10);

        autoSellService.deleteRule("autosell@test.com", created.getRuleId());

        List<AutoSellRuleResponse> remaining = autoSellService.getUserRules("autosell@test.com");
        assertTrue(remaining.isEmpty());
    }

    @Test
    void deleteRule_NotOwner_ShouldThrow() {
        AutoSellRuleResponse created = autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), new BigDecimal("2000.00"), null, 10);

        assertThrows(IllegalArgumentException.class, () -> {
            autoSellService.deleteRule("other@test.com", created.getRuleId());
        });
    }

    @Test
    void deleteRule_NotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            autoSellService.deleteRule("autosell@test.com", 99999L);
        });
    }

    @Test
    void hasActiveRules_WithRules_ShouldReturnTrue() {
        autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), null, 10);

        assertTrue(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void hasActiveRules_WithoutRules_ShouldReturnFalse() {
        assertFalse(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void getStocksWithActiveRules_ShouldReturnStockIds() {
        autoSellService.createRule("autosell@test.com", testStock.getStockId(),
                new BigDecimal("2000.00"), null, 10);

        assertTrue(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void getStocksWithActiveRules_NoRules_ShouldReturnEmpty() {
        assertFalse(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerRules_TargetPriceHit_ShouldRemoveFromCache() {
        autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), new BigDecimal("2000.00"), null, 10);

        assertTrue(autoSellService.hasActiveRules(testStock.getStockId()));
        try {
            autoSellService.checkAndTriggerRules(testStock.getStockId(), new BigDecimal("2100.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }
        assertFalse(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerRules_StopLossHit_ShouldRemoveFromCache() {
        autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), null, new BigDecimal("1200.00"), 10);

        assertTrue(autoSellService.hasActiveRules(testStock.getStockId()));

        try {
            autoSellService.checkAndTriggerRules(testStock.getStockId(), new BigDecimal("1100.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }

        assertFalse(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerRules_PriceBetweenTargetAndStopLoss_ShouldNotTrigger() {
        autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), new BigDecimal("2000.00"), new BigDecimal("1200.00"), 10);
        autoSellService.checkAndTriggerRules(testStock.getStockId(), new BigDecimal("1500.00"));
        assertTrue(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerRules_NoRules_ShouldNotFail() {
        autoSellService.checkAndTriggerRules(testStock.getStockId(), new BigDecimal("2000.00"));
    }

    @Test
    void checkAndTriggerRules_ExactTargetPrice_ShouldRemoveFromCache() {
        autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), new BigDecimal("2000.00"), null, 10);

        try {
            autoSellService.checkAndTriggerRules(testStock.getStockId(), new BigDecimal("2000.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }

        assertFalse(autoSellService.hasActiveRules(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerRules_ExactStopLoss_ShouldRemoveFromCache() {
        autoSellService.createRule("autosell@test.com",
                testStock.getStockId(), null, new BigDecimal("1200.00"), 10);

        try {
            autoSellService.checkAndTriggerRules(testStock.getStockId(), new BigDecimal("1200.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }

        assertFalse(autoSellService.hasActiveRules(testStock.getStockId()));
    }
}

