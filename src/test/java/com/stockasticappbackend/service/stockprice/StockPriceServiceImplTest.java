package com.stockasticappbackend.service.stockprice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
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
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;
import com.stockasticappbackend.service.yahoofinance.YahooQuote;

@SpringBootTest
@Transactional
class StockPriceServiceImplTest {

    @Autowired
    private StockPriceServiceImpl stockPriceService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;

    @MockitoBean
    private YahooFinanceService yahooFinanceService;



    @MockitoBean
    private MarketHoursService marketHoursService;
    
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
    private OrderRepository orderRepository;
    
    @Autowired
    private HoldingsRepository holdingsRepository;
    
    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private KycRepository kycRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private BankAccountRepository bankAccountRepository;

    private Stock testStock;
    private ZonedDateTime now;

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
        orderRepository.deleteAll();
        holdingsRepository.deleteAll();
        transactionRepository.deleteAll();
        
        stockPriceRepository.deleteAll();
        stockRepository.deleteAll();
        stockRepository.flush();
        
        kycRepository.deleteAll();
        walletRepository.deleteAll();
        bankAccountRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.flush();
        ReflectionTestUtils.setField(stockPriceService, "defaultDaysToKeep", 30);

        now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        when(marketHoursService.getCurrentISTTime()).thenReturn(now);
        when(marketHoursService.getLastTradingDay()).thenReturn(now.toLocalDate());
        when(marketHoursService.isTodayTradingDay()).thenReturn(true);

        testStock = new Stock();
        testStock.setSymbol("TCS");
        testStock.setName("Tata Consultancy Services");
        testStock.setExchange("NSE");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);
    }

    @Test
    void getLatestPrice_ExistingData_ShouldReturnResponse() {
        StockPrice price = new StockPrice();
        price.setStock(testStock);
        price.setPrice(new BigDecimal("3500.00"));
        price.setPreviousClose(new BigDecimal("3450.00"));
        price.setOpenPrice(new BigDecimal("3480.00"));
        price.setDayHigh(new BigDecimal("3550.00"));
        price.setDayLow(new BigDecimal("3470.00"));
        price.setVolume(100000L);
        price.setPriceTime(now.toLocalDateTime().withMinute(0).withSecond(0).withNano(0));
        stockPriceRepository.save(price);
        StockPriceResponse response = stockPriceService.getLatestPrice(testStock.getStockId());
        assertNotNull(response);
        assertEquals("TCS", response.getSymbol());
        assertEquals(0, new BigDecimal("3500.00").compareTo(response.getPrice()));
    }

    @Test
    void getLatestPrice_NoData_ShouldFetchFromYahoo() {
        YahooQuote quote = new YahooQuote();
        quote.setSymbol("TCS");
        quote.setPrice(new BigDecimal("3510.00"));
        quote.setPreviousClose(new BigDecimal("3450.00"));
        quote.setOpenPrice(new BigDecimal("3480.00"));
        quote.setDayHigh(new BigDecimal("3550.00"));
        quote.setDayLow(new BigDecimal("3470.00"));
        quote.setVolume(100000L);
        quote.setPriceTime(now.toLocalDateTime().withMinute(0).withSecond(0).withNano(0));
        
        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.singletonList(quote));
        when(yahooFinanceService.getQuote("TCS")).thenReturn(quote);
        
        try {
             stockPriceService.getLatestPrice(testStock.getStockId());
        } catch (ResourceNotFoundException e) {
        }
        StockPriceResponse response = stockPriceService.getLatestPrice(testStock.getStockId());
        
        assertNotNull(response);
        assertEquals(0, new BigDecimal("3510.00").compareTo(response.getPrice()));
        verify(yahooFinanceService).getIntradayQuotes("TCS");
    }

    @Test
    void getAllLatestPrices_ShouldReturnActiveStocksPrices() {
        StockPrice price = new StockPrice();
        price.setStock(testStock);
        price.setPrice(new BigDecimal("3500.00"));
        price.setPriceTime(now.toLocalDateTime().withMinute(0).withSecond(0).withNano(0));
        price.setPreviousClose(new BigDecimal("3400.00"));
        stockPriceRepository.save(price);
        List<StockPriceResponse> responses = stockPriceService.getAllLatestPrices();
        assertEquals(1, responses.size());
        assertEquals("TCS", responses.get(0).getSymbol());
    }

    @Test
    void fetchAndStorePrices_MarketOpen_ShouldFetch() {
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(marketHoursService.getCurrentISTTime()).thenReturn(now.withHour(10).withMinute(0));
        
        YahooQuote quote = new YahooQuote();
        quote.setSymbol("TCS");
        quote.setPrice(new BigDecimal("3520.00"));
        quote.setPriceTime(now.toLocalDateTime().withHour(10).withMinute(0).withSecond(0).withNano(0));
        
        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.singletonList(quote));
        stockPriceService.fetchAndStorePrices();
        List<StockPrice> saved = stockPriceRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(0, new BigDecimal("3520.00").compareTo(saved.get(0).getPrice()));
    }

    @Test
    void fetchAndStorePrices_MarketOpen_NonIntervalMinute_ShouldSkip() {
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(marketHoursService.getCurrentISTTime()).thenReturn(now.withHour(9).withMinute(15).withSecond(10));

        YahooQuote quote = new YahooQuote();
        quote.setSymbol("TCS");
        quote.setPrice(new BigDecimal("3520.00"));
        quote.setPriceTime(now.toLocalDateTime().withHour(9).withMinute(16).withSecond(30).withNano(0));

        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.singletonList(quote));
        stockPriceService.fetchAndStorePrices();

        List<StockPrice> saved = stockPriceRepository.findAll();
        assertTrue(saved.isEmpty());
    }

    @Test
    void fetchAndStorePrices_MarketOpen_ClosedCandle_ShouldPersist() {
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(marketHoursService.getCurrentISTTime()).thenReturn(now.withHour(9).withMinute(15).withSecond(10));

        YahooQuote quote = new YahooQuote();
        quote.setSymbol("TCS");
        quote.setPrice(new BigDecimal("3520.00"));
        quote.setPriceTime(now.toLocalDateTime().withHour(9).withMinute(15).withSecond(30).withNano(0));

        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.singletonList(quote));
        stockPriceService.fetchAndStorePrices();

        List<StockPrice> saved = stockPriceRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(now.toLocalDateTime().withHour(9).withMinute(15).withSecond(0).withNano(0), saved.get(0).getPriceTime());
    }

    @Test
    void fetchAndStorePrices_MarketClosed_ShouldSkip() {
        when(marketHoursService.isMarketOpen()).thenReturn(false);
        when(marketHoursService.getCurrentISTTime()).thenReturn(now.withHour(10).withMinute(0));
        stockPriceService.fetchAndStorePrices();
        verify(yahooFinanceService, never()).getIntradayQuotes(any());
        assertTrue(stockPriceRepository.findAll().isEmpty());
    }

    @Test
    void cleanupOldPrices_ShouldDelete() {
        StockPrice oldPrice = new StockPrice();
        oldPrice.setStock(testStock);
        oldPrice.setPrice(new BigDecimal("3000.00"));
        oldPrice.setPriceTime(now.toLocalDateTime().minusDays(40));
        stockPriceRepository.save(oldPrice);

        StockPrice newPrice = new StockPrice();
        newPrice.setStock(testStock);
        newPrice.setPrice(new BigDecimal("3500.00"));
        newPrice.setPriceTime(now.toLocalDateTime());
        stockPriceRepository.save(newPrice);
        stockPriceService.cleanupOldPrices(30);
        List<StockPrice> remaining = stockPriceRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(0, new BigDecimal("3500.00").compareTo(remaining.get(0).getPrice()));
    }
    
    @Test
    void getTopGainers_ShouldSortCorrectly() {
         Stock stock2 = new Stock();
        stock2.setSymbol("INFY");
        stock2.setName("Infosys");
        stock2.setExchange("NSE");
        stock2.setIsActive(true);
        stock2 = stockRepository.save(stock2);
        StockPrice p1 = new StockPrice();
        p1.setStock(testStock);
        p1.setPrice(new BigDecimal("110.00"));
        p1.setPreviousClose(new BigDecimal("100.00")); 
        p1.setPriceTime(now.toLocalDateTime());
        stockPriceRepository.save(p1);
        StockPrice p2 = new StockPrice();
        p2.setStock(stock2);
        p2.setPrice(new BigDecimal("90.00"));
        p2.setPreviousClose(new BigDecimal("100.00"));
        p2.setPriceTime(now.toLocalDateTime());
        stockPriceRepository.save(p2);
        
        List<StockPriceResponse> gainers = stockPriceService.getTopGainers(5);
        assertEquals("TCS", gainers.get(0).getSymbol());
        
        List<StockPriceResponse> losers = stockPriceService.getTopLosers(5);
        assertEquals("INFY", losers.get(0).getSymbol());
    }
    @Test
    void getPriceHistory_ShouldReturnHistory() {
        StockPrice p1 = new StockPrice();
        p1.setStock(testStock);
        p1.setPrice(new BigDecimal("100.00"));
        p1.setPriceTime(now.toLocalDateTime().minusHours(2));
        stockPriceRepository.save(p1);

        StockPrice p2 = new StockPrice();
        p2.setStock(testStock);
        p2.setPrice(new BigDecimal("105.00"));
        p2.setPriceTime(now.toLocalDateTime().minusHours(1));
        stockPriceRepository.save(p2);
        var history = stockPriceService.getPriceHistory(testStock.getStockId(), 
                now.toLocalDateTime().minusHours(3), now.toLocalDateTime());
        assertNotNull(history);
        assertEquals("TCS", history.getSymbol());
        assertEquals(2, history.getPriceHistory().size());
    }

    @Test
    void forceFetchPrices_ShouldRefreshAllActive() {
        YahooQuote quote = new YahooQuote();
        quote.setSymbol("TCS");
        quote.setPrice(new BigDecimal("3600.00"));
        quote.setPriceTime(now.toLocalDateTime().withMinute(0).withSecond(0).withNano(0));
        
        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.singletonList(quote));
        StockPrice old = new StockPrice();
        old.setStock(testStock);
        old.setPrice(new BigDecimal("9999.00"));
        old.setPriceTime(now.toLocalDateTime().minusDays(1));
        stockPriceRepository.save(old);
        stockPriceService.forceFetchPrices();
        List<StockPrice> saved = stockPriceRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(0, new BigDecimal("3600.00").compareTo(saved.get(0).getPrice()));
    }

    @Test
    void clearPreviousDayPrices_TradingDay_ShouldKeepLatestOnly() {
        when(marketHoursService.isTodayTradingDay()).thenReturn(true);
        when(marketHoursService.getCurrentISTTime()).thenReturn(now.withHour(10).withMinute(0)); 
        StockPrice p1 = new StockPrice();
        p1.setStock(testStock);
        p1.setPrice(new BigDecimal("100.00"));
        p1.setPriceTime(now.toLocalDateTime().minusMinutes(10));
        stockPriceRepository.save(p1);

        StockPrice p2 = new StockPrice();
        p2.setStock(testStock);
        p2.setPrice(new BigDecimal("101.00"));
        p2.setPriceTime(now.toLocalDateTime().minusMinutes(5));
        stockPriceRepository.save(p2);

        StockPrice p3 = new StockPrice();
        p3.setStock(testStock);
        p3.setPrice(new BigDecimal("102.00"));
        p3.setPriceTime(now.toLocalDateTime());
        stockPriceRepository.save(p3);
        stockPriceService.clearPreviousDayPrices();
        List<StockPrice> remaining = stockPriceRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(0, new BigDecimal("102.00").compareTo(remaining.get(0).getPrice()));
    }
    
    @Test
    void clearPreviousDayPrices_Weekend_ShouldDoNothing() {
        when(marketHoursService.isTodayTradingDay()).thenReturn(false);
        
        StockPrice p1 = new StockPrice();
        p1.setStock(testStock);
        p1.setPrice(new BigDecimal("100.00"));
        p1.setPriceTime(now.toLocalDateTime());
        stockPriceRepository.save(p1);
        stockPriceService.clearPreviousDayPrices();
        assertEquals(1, stockPriceRepository.count());
    }

    @Test
    void getLatestPrice_StaleData_ShouldRefresh() {
        StockPrice oldPrice = new StockPrice();
        oldPrice.setStock(testStock);
        oldPrice.setPrice(new BigDecimal("3000.00"));
        oldPrice.setPriceTime(now.toLocalDateTime().minusDays(5));
        stockPriceRepository.save(oldPrice);
        YahooQuote quote = new YahooQuote();
        quote.setSymbol("TCS");
        quote.setPrice(new BigDecimal("3600.00"));
        quote.setPriceTime(now.toLocalDateTime().withMinute(0).withSecond(0).withNano(0));
        
        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.singletonList(quote));
        StockPriceResponse response = stockPriceService.getLatestPrice(testStock.getStockId());
        assertEquals(0, new BigDecimal("3600.00").compareTo(response.getPrice()));
        List<StockPrice> all = stockPriceRepository.findAll();
        assertEquals(0, new BigDecimal("3600.00").compareTo(all.get(0).getPrice()));
    }

    @Test
    void getLatestPrice_FetchFail_ShouldThrow() {
        when(yahooFinanceService.getIntradayQuotes("TCS")).thenReturn(Collections.emptyList());
        when(yahooFinanceService.getQuote("TCS")).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> 
            stockPriceService.getLatestPrice(testStock.getStockId())
        );
    }

    @Test
    void getLatestPrice_InvalidId_ShouldThrow() {
        assertThrows(ResourceNotFoundException.class, () -> 
            stockPriceService.getLatestPrice(99999L)
        );
    }
}


