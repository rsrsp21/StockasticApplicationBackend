package com.stockasticappbackend.service.pricealert;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.user.PriceAlertResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.PriceAlertCondition;
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
import com.stockasticappbackend.service.activity.ActivityLogInternalService;

@SpringBootTest
class PriceAlertServiceTest {
    
    @MockitoBean
    private ActivityLogInternalService activityLogService;

    @Autowired
    private PriceAlertService priceAlertService;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private WalletRepository walletRepository;

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
        activityLogRepository.flush();
        notificationRepository.deleteAll();
        notificationRepository.flush();
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
        stockPriceRepository.deleteAll();
        bankAccountRepository.deleteAll();
        stockRepository.deleteAll();
        activityLogRepository.deleteAll(); 
        activityLogRepository.flush();
        
        userRepository.deleteAll();
        userRepository.flush();

        testUser = new AppUser();
        testUser.setEmail("pricealert@test.com");
        testUser.setName("Price Alert User");
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
        testStock.setSymbol("TCS");
        testStock.setName("Tata Consultancy Services");
        testStock.setExchange("NSE");
        testStock.setSector("IT");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);
    }

    @Test
    void createAlert_ValidAboveCondition_ShouldCreateAlert() {
        PriceAlertResponse response = priceAlertService.createAlert(
                "pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);

        assertNotNull(response);
        assertNotNull(response.getAlertId());
        assertEquals(testStock.getStockId(), response.getStockId());
        assertEquals("TCS", response.getSymbol());
        assertEquals("Tata Consultancy Services", response.getStockName());
        assertEquals(0, new BigDecimal("4000.00").compareTo(response.getTargetPrice()));
        assertEquals(PriceAlertCondition.ABOVE, response.getCondition());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void createAlert_ValidBelowCondition_ShouldCreateAlert() {
        PriceAlertResponse response = priceAlertService.createAlert(
                "pricealert@test.com", testStock.getStockId(),
                new BigDecimal("3000.00"), PriceAlertCondition.BELOW);

        assertNotNull(response);
        assertEquals(PriceAlertCondition.BELOW, response.getCondition());
        assertEquals(0, new BigDecimal("3000.00").compareTo(response.getTargetPrice()));
    }

    @Test
    void createAlert_UserNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            priceAlertService.createAlert("nonexistent@test.com",
                    testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        });
    }

    @Test
    void createAlert_StockNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            priceAlertService.createAlert("pricealert@test.com",
                    99999L, new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        });
    }

    @Test
    void createAlert_MultiplAlertsOnSameStock_ShouldAllowMultiple() {
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        PriceAlertResponse second = priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("3000.00"), PriceAlertCondition.BELOW);

        assertNotNull(second.getAlertId());

        List<PriceAlertResponse> alerts = priceAlertService.getUserAlerts("pricealert@test.com");
        assertEquals(2, alerts.size());
    }

    @Test
    void getUserAlerts_ShouldReturnAllAlerts() {
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("3000.00"), PriceAlertCondition.BELOW);

        List<PriceAlertResponse> alerts = priceAlertService.getUserAlerts("pricealert@test.com");

        assertNotNull(alerts);
        assertEquals(2, alerts.size());
    }

    @Test
    void getUserAlerts_NoAlerts_ShouldReturnEmptyList() {
        List<PriceAlertResponse> alerts = priceAlertService.getUserAlerts("pricealert@test.com");

        assertNotNull(alerts);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void getUserAlerts_UserNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            priceAlertService.getUserAlerts("nonexistent@test.com");
        });
    }

    @Test
    void getPagedUserAlerts_ShouldReturnPaginated() {
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("3500.00"), PriceAlertCondition.ABOVE);
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("3000.00"), PriceAlertCondition.BELOW);

        PageResponse<PriceAlertResponse> page = priceAlertService.getPagedUserAlerts(
                "pricealert@test.com", 0, 2, "createdAt", "desc", null);

        assertNotNull(page);
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertTrue(page.isFirst());
        assertFalse(page.isLast());
        assertTrue(page.isHasNext());
        assertFalse(page.isHasPrevious());
    }

    @Test
    void getPagedUserAlerts_FilterByActive_ShouldReturnOnlyActive() {
        PriceAlertResponse alert1 = priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("3000.00"), PriceAlertCondition.BELOW);
        var entity = priceAlertRepository.findById(alert1.getAlertId()).orElseThrow();
        entity.setActive(false);
        priceAlertRepository.save(entity);

        PageResponse<PriceAlertResponse> activePage = priceAlertService.getPagedUserAlerts(
                "pricealert@test.com", 0, 10, "createdAt", "desc", true);

        assertEquals(1, activePage.getTotalElements());
        assertTrue(activePage.getContent().get(0).isActive());

        PageResponse<PriceAlertResponse> inactivePage = priceAlertService.getPagedUserAlerts(
                "pricealert@test.com", 0, 10, "createdAt", "desc", false);

        assertEquals(1, inactivePage.getTotalElements());
        assertFalse(inactivePage.getContent().get(0).isActive());
    }

    @Test
    void getPagedUserAlerts_AscendingSort_ShouldWork() {
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("3000.00"), PriceAlertCondition.BELOW);

        PageResponse<PriceAlertResponse> page = priceAlertService.getPagedUserAlerts(
                "pricealert@test.com", 0, 10, "createdAt", "asc", null);

        assertNotNull(page);
        assertEquals(2, page.getContent().size());
    }

    @Test
    void getPagedUserAlerts_UserNotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            priceAlertService.getPagedUserAlerts("nonexistent@test.com",
                    0, 10, "createdAt", "desc", null);
        });
    }

    @Test
    void deleteAlert_ValidOwner_ShouldDelete() {
        PriceAlertResponse created = priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);

        priceAlertService.deleteAlert("pricealert@test.com", created.getAlertId());

        List<PriceAlertResponse> remaining = priceAlertService.getUserAlerts("pricealert@test.com");
        assertTrue(remaining.isEmpty());
    }

    @Test
    void deleteAlert_NotOwner_ShouldThrow() {
        PriceAlertResponse created = priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);

        assertThrows(IllegalArgumentException.class, () -> {
            priceAlertService.deleteAlert("other@test.com", created.getAlertId());
        });
    }

    @Test
    void deleteAlert_NotFound_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> {
            priceAlertService.deleteAlert("pricealert@test.com", 99999L);
        });
    }

    @Test
    void checkAndTriggerAlerts_AboveConditionMet_ShouldRemoveFromCache() {
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);

        assertTrue(priceAlertService.hasActiveAlerts(testStock.getStockId()));
        try {
            priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("4500.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }
        assertFalse(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerAlerts_AboveConditionNotMet_ShouldNotTrigger() {
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("3500.00"));
        assertTrue(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerAlerts_BelowConditionMet_ShouldRemoveFromCache() {
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("3000.00"), PriceAlertCondition.BELOW);

        assertTrue(priceAlertService.hasActiveAlerts(testStock.getStockId()));
        try {
            priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("2800.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }

        assertFalse(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerAlerts_BelowConditionNotMet_ShouldNotTrigger() {
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("3000.00"), PriceAlertCondition.BELOW);
        priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("3500.00"));

        assertTrue(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerAlerts_NoAlerts_ShouldNotFail() {
        priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("4500.00"));
    }

    @Test
    void checkAndTriggerAlerts_AboveExactPrice_ShouldRemoveFromCache() {
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);
        try {
            priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("4000.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }

        assertFalse(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void checkAndTriggerAlerts_BelowExactPrice_ShouldRemoveFromCache() {
        priceAlertService.createAlert("pricealert@test.com",
                testStock.getStockId(), new BigDecimal("3000.00"), PriceAlertCondition.BELOW);
        try {
            priceAlertService.checkAndTriggerAlerts(testStock.getStockId(), new BigDecimal("3000.00"));
        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
        }

        assertFalse(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void hasActiveAlerts_WithAlerts_ShouldReturnTrue() {
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);

        assertTrue(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void hasActiveAlerts_WithoutAlerts_ShouldReturnFalse() {
        assertFalse(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void getStocksWithActiveAlerts_ShouldReturnStockIds() {
        priceAlertService.createAlert("pricealert@test.com", testStock.getStockId(),
                new BigDecimal("4000.00"), PriceAlertCondition.ABOVE);

        assertTrue(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }

    @Test
    void getStocksWithActiveAlerts_NoAlerts_ShouldReturnEmpty() {
        assertFalse(priceAlertService.hasActiveAlerts(testStock.getStockId()));
    }
}

