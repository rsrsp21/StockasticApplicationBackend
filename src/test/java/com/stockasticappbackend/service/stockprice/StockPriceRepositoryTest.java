package com.stockasticappbackend.service.stockprice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
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

/**
 * Tests for StockPrice repository operations and data layer.
 * 
 * Note: StockPriceServiceImpl is stubbed in TestConfig for other tests.
 * This test focuses on the repository queries and stock price data operations
 * that ARE testable without Yahoo Finance external calls.
 */
@SpringBootTest
class StockPriceRepositoryTest {

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private StockRepository stockRepository;

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
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Stock testStock;
    private Stock testStock2;

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
        stockRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        testStock = new Stock();
        testStock.setSymbol("TCS");
        testStock.setName("Tata Consultancy Services");
        testStock.setExchange("NSE");
        testStock.setSector("IT");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);

        testStock2 = new Stock();
        testStock2.setSymbol("INFY");
        testStock2.setName("Infosys Limited");
        testStock2.setExchange("NSE");
        testStock2.setSector("IT");
        testStock2.setIsActive(true);
        testStock2 = stockRepository.save(testStock2);
    }

    /**
     * Helper to create a stock price record.
     */
    private StockPrice createStockPrice(Stock stock, BigDecimal price, BigDecimal openPrice,
                                         BigDecimal previousClose, BigDecimal dayHigh,
                                         BigDecimal dayLow, Long volume, LocalDateTime priceTime) {
        StockPrice sp = StockPrice.builder()
                .stock(stock)
                .price(price)
                .openPrice(openPrice)
                .previousClose(previousClose)
                .dayHigh(dayHigh)
                .dayLow(dayLow)
                .volume(volume)
                .priceTime(priceTime)
                .build();
        return stockPriceRepository.save(sp);
    }

    @Test
    void findLatestByStockId_ShouldReturnMostRecent() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusMinutes(10));

        createStockPrice(testStock, new BigDecimal("3520.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                120000L, now.minusMinutes(5));

        Optional<StockPrice> latest = stockPriceRepository.findLatestByStockId(testStock.getStockId());

        assertTrue(latest.isPresent());
        assertEquals(0, new BigDecimal("3520.00").compareTo(latest.get().getPrice()));
    }

    @Test
    void findLatestByStockId_NoPrices_ShouldReturnEmpty() {
        Optional<StockPrice> latest = stockPriceRepository.findLatestByStockId(testStock.getStockId());
        assertFalse(latest.isPresent());
    }

    @Test
    void findLatestBySymbol_ShouldReturnLatest() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusMinutes(5));

        Optional<StockPrice> latest = stockPriceRepository.findLatestBySymbol("TCS");

        assertTrue(latest.isPresent());
        assertEquals(0, new BigDecimal("3500.00").compareTo(latest.get().getPrice()));
    }

    @Test
    void findLatestBySymbol_NonExistentSymbol_ShouldReturnEmpty() {
        Optional<StockPrice> latest = stockPriceRepository.findLatestBySymbol("NONEXISTENT");
        assertFalse(latest.isPresent());
    }

    @Test
    @Transactional
    void findLatestPricesForActiveStocks_ShouldReturnOnePerStock() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusMinutes(10));
        createStockPrice(testStock, new BigDecimal("3520.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                120000L, now.minusMinutes(5));
        createStockPrice(testStock2, new BigDecimal("1800.00"), new BigDecimal("1750.00"),
                new BigDecimal("1740.00"), new BigDecimal("1820.00"), new BigDecimal("1730.00"),
                80000L, now.minusMinutes(10));
        createStockPrice(testStock2, new BigDecimal("1810.00"), new BigDecimal("1750.00"),
                new BigDecimal("1740.00"), new BigDecimal("1820.00"), new BigDecimal("1730.00"),
                90000L, now.minusMinutes(5));

        List<StockPrice> latestPrices = stockPriceRepository.findLatestPricesForActiveStocks();

        assertEquals(2, latestPrices.size());
        for (StockPrice sp : latestPrices) {
            if (sp.getStock().getSymbol().equals("TCS")) {
                assertEquals(0, new BigDecimal("3520.00").compareTo(sp.getPrice()));
            } else if (sp.getStock().getSymbol().equals("INFY")) {
                assertEquals(0, new BigDecimal("1810.00").compareTo(sp.getPrice()));
            }
        }
    }

    @Test
    @Transactional
    void findLatestPricesForActiveStocks_InactiveStock_ShouldBeExcluded() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusMinutes(5));
        testStock2.setIsActive(false);
        stockRepository.save(testStock2);

        createStockPrice(testStock2, new BigDecimal("1800.00"), new BigDecimal("1750.00"),
                new BigDecimal("1740.00"), new BigDecimal("1820.00"), new BigDecimal("1730.00"),
                80000L, now.minusMinutes(5));

        List<StockPrice> latestPrices = stockPriceRepository.findLatestPricesForActiveStocks();
        assertEquals(1, latestPrices.size());
        assertEquals("TCS", latestPrices.get(0).getStock().getSymbol());
    }

    @Test
    void findPriceHistoryByStockId_ShouldReturnInRange() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusHours(3));
        createStockPrice(testStock, new BigDecimal("3510.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                110000L, now.minusHours(2));
        createStockPrice(testStock, new BigDecimal("3520.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                120000L, now.minusHours(1));
        List<StockPrice> history = stockPriceRepository.findPriceHistoryByStockId(
                testStock.getStockId(), now.minusHours(2).minusMinutes(30), now);

        assertEquals(2, history.size());
        assertTrue(history.get(0).getPriceTime().isBefore(history.get(1).getPriceTime()));
    }

    @Test
    void findPriceHistoryByStockId_EmptyRange_ShouldReturnEmpty() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now);
        List<StockPrice> history = stockPriceRepository.findPriceHistoryByStockId(
                testStock.getStockId(), now.minusDays(5), now.minusDays(4));

        assertTrue(history.isEmpty());
    }

    @Test
    void existsByStockAndPriceTime_ShouldDetectDuplicates() {
        LocalDateTime priceTime = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, priceTime);

        assertTrue(stockPriceRepository.existsByStockAndPriceTime(testStock, priceTime));
        assertFalse(stockPriceRepository.existsByStockAndPriceTime(testStock, priceTime.plusMinutes(5)));
    }

    @Test
    void countByStockIdAndDate_ShouldCountCorrectly() {
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, today);
        createStockPrice(testStock, new BigDecimal("3510.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                110000L, today.plusMinutes(5));

        long count = stockPriceRepository.countByStockIdAndDate(testStock.getStockId(),
                today.toLocalDate());
        assertEquals(2, count);
    }

    @Test
    void existsByStockIdAndDate_ShouldReturnCorrectly() {
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, today);

        assertTrue(stockPriceRepository.existsByStockIdAndDate(testStock.getStockId(),
                today.toLocalDate()));
        assertFalse(stockPriceRepository.existsByStockIdAndDate(testStock.getStockId(),
                today.toLocalDate().minusDays(5)));
    }

    @Test
    @Transactional
    void deleteByStock_ShouldDeleteAllPrices() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusMinutes(10));
        createStockPrice(testStock, new BigDecimal("3520.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                120000L, now.minusMinutes(5));

        stockPriceRepository.deleteByStock(testStock);

        assertEquals(0, stockPriceRepository.countByStockIdAndDate(testStock.getStockId(),
                now.toLocalDate()));
    }

    @Test
    @Transactional
    void deleteByPriceTimeBefore_ShouldDeleteOldRecords() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusDays(10));
        createStockPrice(testStock, new BigDecimal("3520.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                120000L, now);

        int deleted = stockPriceRepository.deleteByPriceTimeBefore(now.minusDays(5));

        assertEquals(1, deleted);
        assertEquals(1, stockPriceRepository.findAll().size());
    }

    @Test
    @Transactional
    void deleteByStockAndPriceIdNot_ShouldKeepOnlyAnchor() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        StockPrice keep = createStockPrice(testStock, new BigDecimal("3520.00"),
                new BigDecimal("3400.00"), new BigDecimal("3380.00"), new BigDecimal("3550.00"),
                new BigDecimal("3350.00"), 120000L, now);
        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusMinutes(5));

        int deleted = stockPriceRepository.deleteByStockAndPriceIdNot(testStock, keep.getPriceId());

        assertEquals(1, deleted);

        var remaining = stockPriceRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(keep.getPriceId(), remaining.get(0).getPriceId());
    }

    @Test
    void sumVolumeByStockIdAndDate_ShouldSumCorrectly() {
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                50000L, today);
        createStockPrice(testStock, new BigDecimal("3510.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                75000L, today.plusMinutes(5));

        Long total = stockPriceRepository.sumVolumeByStockIdAndDate(
                testStock.getStockId(), today.toLocalDate());

        assertEquals(125000L, total);
    }

    @Test
    void getAvgVolumeByStockIdAndDate_ShouldAverageCorrectly() {
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                50000L, today);
        createStockPrice(testStock, new BigDecimal("3510.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, today.plusMinutes(5));

        Double avg = stockPriceRepository.getAvgVolumeByStockIdAndDate(
                testStock.getStockId(), today.toLocalDate());

        assertNotNull(avg);
        assertEquals(75000.0, avg, 0.1);
    }

    @Test
    void findByStockOrderByPriceTimeDesc_ShouldReturnPaginated() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        for (int i = 0; i < 5; i++) {
            createStockPrice(testStock, new BigDecimal("3500.00").add(BigDecimal.valueOf(i * 10)),
                    new BigDecimal("3400.00"), new BigDecimal("3380.00"),
                    new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                    100000L + (i * 1000L), now.minusMinutes(i * 5));
        }

        var page = stockPriceRepository.findByStockOrderByPriceTimeDesc(testStock, PageRequest.of(0, 3));

        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertTrue(page.getContent().get(0).getPriceTime()
                .isAfter(page.getContent().get(1).getPriceTime()));
    }

    @Test
    void findByStockIdAndDate_ShouldReturnPricesForSpecificDate() {
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterday = today.minusDays(1);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, today);
        createStockPrice(testStock, new BigDecimal("3480.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                90000L, yesterday);

        List<StockPrice> todayPrices = stockPriceRepository.findByStockIdAndDate(
                testStock.getStockId(), today.toLocalDate());

        assertEquals(1, todayPrices.size());
        assertEquals(0, new BigDecimal("3500.00").compareTo(todayPrices.get(0).getPrice()));
    }

    @Test
    void findOldestByStockId_ShouldReturnOldest() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);

        createStockPrice(testStock, new BigDecimal("3500.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                100000L, now.minusHours(3));
        createStockPrice(testStock, new BigDecimal("3520.00"), new BigDecimal("3400.00"),
                new BigDecimal("3380.00"), new BigDecimal("3550.00"), new BigDecimal("3350.00"),
                120000L, now);

        Optional<StockPrice> oldest = stockPriceRepository.findOldestByStockId(testStock.getStockId());

        assertTrue(oldest.isPresent());
        assertEquals(0, new BigDecimal("3500.00").compareTo(oldest.get().getPrice()));
    }
}


