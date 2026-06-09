package com.stockasticappbackend.service.holdings;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.stockasticappbackend.config.TestConfig;
import com.stockasticappbackend.dto.order.HoldingsResponse;
import com.stockasticappbackend.exception.InsufficientHoldingsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Holdings;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.HoldingsRepository;
import com.stockasticappbackend.repository.NotificationRepository;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.PriceAlertRepository;
import com.stockasticappbackend.repository.SipRepository;
import com.stockasticappbackend.repository.StockIndicatorRepository;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WatchlistItemRepository;
import com.stockasticappbackend.repository.WatchlistRepository;

@SpringBootTest
@Import(TestConfig.class)
class HoldingsServiceImplTest {

    @Autowired
    private HoldingsService holdingsService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private AppUser testUser;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        stockIndicatorRepository.deleteAll();
        watchlistItemRepository.deleteAll();
        watchlistRepository.deleteAll();
        priceAlertRepository.deleteAll();
        sipRepository.deleteAll();
        autoSellRuleRepository.deleteAll();
        orderRepository.deleteAll();
        holdingsRepository.deleteAll();
        stockPriceRepository.deleteAll();
        stockRepository.deleteAll();
        stockRepository.flush();
        notificationRepository.deleteAll();
        activityLogRepository.deleteAll();
        activityLogRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        testUser = new AppUser();
        testUser.setEmail("holdings@example.com");
        testUser.setName("Holdings User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        testStock = new Stock();
        testStock.setSymbol("HOLD_TEST");
        testStock.setName("Holdings Test Stock");
        testStock.setExchange("NSE");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);
    }

    @Test
    void creditHoldings_NewHolding_ShouldCreateEntry() {
        BigDecimal totalCost = new BigDecimal("15000.00");
        holdingsService.creditHoldings(testUser, testStock, 10, totalCost);
        Holdings holdings = holdingsRepository.findByUserIdAndStockId(
                testUser.getUserId(), testStock.getStockId()).orElse(null);
        assertNotNull(holdings);
        assertEquals(10, holdings.getQuantity());
        assertEquals(0, new BigDecimal("1500.0000").compareTo(holdings.getAveragePrice()));
    }

    @Test
    void creditHoldings_ExistingHolding_ShouldUpdateAveragePrice() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("15000.00"));
        holdingsService.creditHoldings(testUser, testStock, 5, new BigDecimal("8000.00"));
        Holdings holdings = holdingsRepository.findByUserIdAndStockId(
                testUser.getUserId(), testStock.getStockId()).orElse(null);
        assertNotNull(holdings);
        assertEquals(15, holdings.getQuantity());
        BigDecimal expectedAvg = new BigDecimal("23000.00")
                .divide(new BigDecimal("15"), 4, RoundingMode.HALF_UP);
        assertEquals(0, expectedAvg.compareTo(holdings.getAveragePrice()));
    }

    @Test
    void debitHoldings_PartialSell_ShouldReduceQuantity() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("15000.00"));
        holdingsService.blockShares(testUser, testStock, 5);
        BigDecimal realizedPnl = holdingsService.debitHoldings(
                testUser, testStock, 5, new BigDecimal("8500.00"), new BigDecimal("1500.00"));
        Holdings holdings = holdingsRepository.findByUserIdAndStockId(
                testUser.getUserId(), testStock.getStockId()).orElse(null);
        assertNotNull(holdings);
        assertEquals(5, holdings.getQuantity());
        assertEquals(0, holdings.getLockedQuantity());
        assertEquals(0, new BigDecimal("1000.00").compareTo(realizedPnl));
    }

    @Test
    void debitHoldings_FullSell_ShouldDeleteHolding() {
        holdingsService.creditHoldings(testUser, testStock, 5, new BigDecimal("5000.00"));
        holdingsService.blockShares(testUser, testStock, 5);
        holdingsService.debitHoldings(
                testUser, testStock, 5, new BigDecimal("6000.00"), new BigDecimal("1000.00"));
        assertFalse(holdingsRepository.findByUserIdAndStockId(
                testUser.getUserId(), testStock.getStockId()).isPresent());
    }

    @Test
    void blockShares_ShouldIncreaseLockedQuantity() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("10000.00"));
        holdingsService.blockShares(testUser, testStock, 3);
        Holdings holdings = holdingsRepository.findByUserIdAndStockId(
                testUser.getUserId(), testStock.getStockId()).orElse(null);
        assertNotNull(holdings);
        assertEquals(3, holdings.getLockedQuantity());
        assertEquals(7, holdings.getAvailableQuantity());
    }

    @Test
    void releaseBlockedShares_ShouldDecreaseLockedQuantity() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("10000.00"));
        holdingsService.blockShares(testUser, testStock, 5);
        holdingsService.releaseBlockedShares(testUser, testStock, 3);
        Holdings holdings = holdingsRepository.findByUserIdAndStockId(
                testUser.getUserId(), testStock.getStockId()).orElse(null);
        assertNotNull(holdings);
        assertEquals(2, holdings.getLockedQuantity());
        assertEquals(8, holdings.getAvailableQuantity());
    }

    @Test
    void validateHoldingsForSell_SufficientShares_ShouldNotThrow() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("10000.00"));
        assertDoesNotThrow(() -> {
            holdingsService.validateHoldingsForSell(testUser, testStock, 5);
        });
    }

    @Test
    void validateHoldingsForSell_InsufficientShares_ShouldThrow() {
        holdingsService.creditHoldings(testUser, testStock, 5, new BigDecimal("5000.00"));
        assertThrows(InsufficientHoldingsException.class, () -> {
            holdingsService.validateHoldingsForSell(testUser, testStock, 10);
        });
    }

    @Test
    void validateHoldingsForSell_NoHoldings_ShouldThrow() {
        assertThrows(InsufficientHoldingsException.class, () -> {
            holdingsService.validateHoldingsForSell(testUser, testStock, 1);
        });
    }

    @Test
    void validateHoldingsForSell_SharesLocked_ShouldThrowIfAvailableInsufficient() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("10000.00"));
        holdingsService.blockShares(testUser, testStock, 8);
        assertThrows(InsufficientHoldingsException.class, () -> {
            holdingsService.validateHoldingsForSell(testUser, testStock, 5);
        });
    }

    @Test
    void getAveragePrice_ShouldReturnCorrectPrice() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("15000.00"));
        BigDecimal avgPrice = holdingsService.getAveragePrice(testUser, testStock);
        assertEquals(0, new BigDecimal("1500.0000").compareTo(avgPrice));
    }

    @Test
    void getHoldings_ShouldReturnUserHoldings() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("10000.00"));

        Stock stock2 = new Stock();
        stock2.setSymbol("HOLD_TST2");
        stock2.setName("Test Stock 2");
        stock2.setExchange("NSE");
        stock2.setIsActive(true);
        stock2 = stockRepository.save(stock2);
        holdingsService.creditHoldings(testUser, stock2, 5, new BigDecimal("7500.00"));
        List<HoldingsResponse> holdings = holdingsService.getHoldings("holdings@example.com");
        assertEquals(2, holdings.size());
    }

    @Test
    void getHoldings_NoHoldings_ShouldReturnEmptyList() {
        List<HoldingsResponse> holdings = holdingsService.getHoldings("holdings@example.com");
        assertNotNull(holdings);
        assertTrue(holdings.isEmpty());
    }

    @Test
    void getHoldings_UserNotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            holdingsService.getHoldings("nonexistent@example.com");
        });
    }

    @Test
    void getHoldingByStock_Exists_ShouldReturnResponse() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("10000.00"));
        HoldingsResponse response = holdingsService.getHoldingByStock(
                "holdings@example.com", testStock.getStockId());
        assertNotNull(response);
        assertEquals(10, response.getQuantity());
    }

    @Test
    void getHoldingByStock_NotExists_ShouldReturnNull() {
        HoldingsResponse response = holdingsService.getHoldingByStock(
                "holdings@example.com", testStock.getStockId());
        assertNull(response);
    }

    @Test
    void debitHoldings_LossScenario_ShouldReturnNegativePnl() {
        holdingsService.creditHoldings(testUser, testStock, 10, new BigDecimal("15000.00"));
        holdingsService.blockShares(testUser, testStock, 5);
        BigDecimal realizedPnl = holdingsService.debitHoldings(
                testUser, testStock, 5, new BigDecimal("6000.00"), new BigDecimal("1500.00"));
        assertTrue(realizedPnl.compareTo(BigDecimal.ZERO) < 0);
        assertEquals(0, new BigDecimal("-1500.00").compareTo(realizedPnl));
    }
}


