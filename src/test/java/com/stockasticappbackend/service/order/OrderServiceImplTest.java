package com.stockasticappbackend.service.order;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.exception.InsufficientFundsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Holdings;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.entity.Order;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderStatus;
import com.stockasticappbackend.model.enums.OrderType;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
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
import com.stockasticappbackend.service.stockprice.MarketHoursService;
import com.stockasticappbackend.service.yahoofinance.YahooFinanceService;

@SpringBootTest
class OrderServiceImplTest {

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @MockitoBean
    private MarketHoursService marketHoursService;
    
    @MockitoBean
    private YahooFinanceService yahooFinanceService;

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
        transactionRepository.deleteAll();
        orderRepository.deleteAll();
        holdingsRepository.deleteAll();
        stockPriceRepository.deleteAll();
        kycRepository.deleteAll();
        walletRepository.deleteAll();
        stockRepository.deleteAll();
        userRepository.deleteAll();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(marketHoursService.isTodayTradingDay()).thenReturn(true);
        when(marketHoursService.isBeforeMarketOpen()).thenReturn(false);
        when(marketHoursService.isAfterMarketClose()).thenReturn(false);
        when(marketHoursService.getCurrentISTTime()).thenReturn(now);
        when(marketHoursService.getLastTradingDay()).thenReturn(now.toLocalDate());
        testUser = new AppUser();
        testUser.setEmail("order@example.com");
        testUser.setName("Order User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.APPROVED);
        kyc.setPanNumber("ABCDE1234F");
        kyc.setAadhaarNumber("123456789012");
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
        testStock = new Stock();
        testStock.setSymbol("ORD_TEST");
        testStock.setName("Order Test Stock");
        testStock.setExchange("NSE");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);
        StockPrice price = StockPrice.builder()
                .stock(testStock)
                .price(new BigDecimal("100.0000"))
                .openPrice(new BigDecimal("98.0000"))
                .previousClose(new BigDecimal("95.0000"))
                .priceTime(now.toLocalDateTime())
                .volume(10000L)
                .build();
        stockPriceRepository.save(price);
        Wallet wallet = new Wallet();
        wallet.setUser(testUser);
        wallet.setAvailableBalance(new BigDecimal("100000.00"));
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        walletRepository.save(wallet);
    }

    @Test
    void placeOrder_BuyMarket_ShouldCreateOrder() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        OrderResponse response = orderService.placeOrder("order@example.com", request);
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(OrderType.BUY, response.getOrderType());
        assertEquals(OrderMode.MARKET, response.getOrderMode());
        assertEquals(10, response.getQuantity());
        assertEquals(OrderStatus.FILLED, response.getStatus());
    }

    @Test
    void placeOrder_BuyMarket_ShouldDeductWalletBalance() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        orderService.placeOrder("order@example.com", request);
        Wallet wallet = walletRepository.findByUser(testUser).get();
        assertTrue(wallet.getAvailableBalance().compareTo(new BigDecimal("100000.00")) < 0);
    }

    @Test
    void placeOrder_InsufficientFunds_ShouldThrowException() {
        Wallet wallet = walletRepository.findByUser(testUser).get();
        wallet.setAvailableBalance(new BigDecimal("10.00"));
        walletRepository.save(wallet);

        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(100);
        assertThrows(InsufficientFundsException.class, () -> {
            orderService.placeOrder("order@example.com", request);
        });
    }

    @Test
    void placeOrder_NoKyc_ShouldThrowException() {
        AppUser noKycUser = new AppUser();
        noKycUser.setEmail("nokyc@example.com");
        noKycUser.setName("No KYC User");
        noKycUser.setPasswordHash("hash");
        noKycUser.setUserStatus(UserStatus.ACTIVE);
        noKycUser = userRepository.save(noKycUser);

        Wallet wallet = new Wallet();
        wallet.setUser(noKycUser);
        wallet.setAvailableBalance(new BigDecimal("100000.00"));
        wallet.setLockedBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        assertThrows(IllegalStateException.class, () -> {
            orderService.placeOrder("nokyc@example.com", request);
        });
    }

    @Test
    void placeOrder_LimitWithoutPrice_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.LIMIT);
        request.setQuantity(10);
        request.setPrice(null);
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder("order@example.com", request);
        });
    }

    @Test
    void placeOrder_BuyLimit_ExecuteImmediately() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.LIMIT);
        request.setQuantity(10);
        request.setPrice(new BigDecimal("105.00"));
        OrderResponse response = orderService.placeOrder("order@example.com", request);
        assertEquals(OrderStatus.FILLED, response.getStatus());
    }
    
    @Test
    void placeOrder_BuyLimit_Pending() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.LIMIT);
        request.setQuantity(10);
        request.setPrice(new BigDecimal("95.00"));
        OrderResponse response = orderService.placeOrder("order@example.com", request);
        assertEquals(OrderStatus.PENDING, response.getStatus());
    }

    @Test
    void placeOrder_SellMarket_ShouldWorkWithHoldings() {
        Holdings holdings = Holdings.builder()
                .user(testUser)
                .stock(testStock)
                .quantity(50)
                .lockedQuantity(0)
                .averagePrice(new BigDecimal("90.0000"))
                .build();
        holdingsRepository.save(holdings);

        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.SELL);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        OrderResponse response = orderService.placeOrder("order@example.com", request);
        assertNotNull(response);
        assertEquals(OrderType.SELL, response.getOrderType());
        assertEquals(OrderStatus.FILLED, response.getStatus());
    }
    
    @Test
    void placeOrder_SellLimit_Pending() {
        Holdings holdings = Holdings.builder()
                .user(testUser)
                .stock(testStock)
                .quantity(50)
                .lockedQuantity(0)
                .averagePrice(new BigDecimal("90.0000"))
                .build();
        holdingsRepository.save(holdings);
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.SELL);
        request.setOrderMode(OrderMode.LIMIT);
        request.setQuantity(10);
        request.setPrice(new BigDecimal("110.00"));
        OrderResponse response = orderService.placeOrder("order@example.com", request);
        assertEquals(OrderStatus.PENDING, response.getStatus());
    }

    @Test
    void placeOrder_SellWithoutHoldings_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.SELL);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        assertThrows(Exception.class, () -> {
            orderService.placeOrder("order@example.com", request);
        });
    }

    @Test
    void getOrderHistory_ShouldReturnOrders() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(5);
        orderService.placeOrder("order@example.com", request);
        List<OrderResponse> history = orderService.getOrderHistory("order@example.com");
        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
    }

    @Test
    void getOrderHistory_Empty_ShouldReturnEmptyList() {
        List<OrderResponse> history = orderService.getOrderHistory("order@example.com");
        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void getOrder_ShouldReturnSpecificOrder() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(5);
        OrderResponse placed = orderService.placeOrder("order@example.com", request);
        OrderResponse retrieved = orderService.getOrder("order@example.com", placed.getOrderId());
        assertNotNull(retrieved);
        assertEquals(placed.getOrderId(), retrieved.getOrderId());
    }

    @Test
    void getOrder_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrder("order@example.com", 999999L);
        });
    }

    @Test
    void getOrder_WrongUser_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(5);
        OrderResponse placed = orderService.placeOrder("order@example.com", request);
        AppUser otherUser = new AppUser();
        otherUser.setEmail("other_order@example.com");
        otherUser.setName("Other");
        otherUser.setPasswordHash("hash");
        userRepository.save(otherUser);

        Long orderId = placed.getOrderId();
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrder("other_order@example.com", orderId);
        });
    }

    @Test
    void getMostTradedStocks_ShouldReturnList() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(5);
        orderService.placeOrder("order@example.com", request);
        var mostTraded = orderService.getMostTradedStocks(5);
        assertNotNull(mostTraded);
    }

    @Test
    void placeOrder_UserNotFound_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(5);
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.placeOrder("nonexistent@example.com", request);
        });
    }
    
    @Test
    void placeOrder_StockNotFound_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(999999L);
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(5);
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.placeOrder("order@example.com", request);
        });
    }

    @Test
    void cancelOrder_PendingBuy_ShouldRefundAndCancel() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.LIMIT);
        request.setQuantity(10);
        request.setPrice(new BigDecimal("50.00"));
        
        OrderResponse created = orderService.placeOrder("order@example.com", request);
        Long orderId = created.getOrderId();
        assertEquals(OrderStatus.PENDING, created.getStatus());
        
        Wallet walletBefore = walletRepository.findByUser(testUser).get();
        BigDecimal lockedBefore = walletBefore.getLockedBalance();
        assertTrue(lockedBefore.compareTo(BigDecimal.ZERO) > 0);
        OrderResponse cancelled = orderService.cancelOrder("order@example.com", orderId);
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
        
        Wallet walletAfter = walletRepository.findByUser(testUser).get();
        assertTrue(walletAfter.getLockedBalance().compareTo(lockedBefore) < 0);
    }
    
    @Test
    void cancelOrder_Filled_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        OrderResponse created = orderService.placeOrder("order@example.com", request);
        assertThrows(IllegalStateException.class, () -> {
             orderService.cancelOrder("order@example.com", created.getOrderId());
        });
    }
    
    @Test
    void cancelOrder_WrongUser_ShouldThrowException() {
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.LIMIT);
        request.setQuantity(10);
        request.setPrice(new BigDecimal("50"));
        OrderResponse created = orderService.placeOrder("order@example.com", request);
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.cancelOrder("nokyc@example.com", created.getOrderId());
        });
    }

    @Test
    void processDailyMarketOpenOrders_ShouldExecuteAmo() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
        when(marketHoursService.getCurrentISTTime()).thenReturn(now);
        when(marketHoursService.getLastTradingDay()).thenReturn(now.toLocalDate());
        
        when(marketHoursService.isMarketOpen()).thenReturn(false);
        when(marketHoursService.isBeforeMarketOpen()).thenReturn(true); 
        OrderRequest request = new OrderRequest();
        request.setStockId(testStock.getStockId());
        request.setOrderType(OrderType.BUY);
        request.setOrderMode(OrderMode.MARKET);
        request.setQuantity(10);
        OrderResponse amo = orderService.placeOrder("order@example.com", request);
        assertEquals(OrderStatus.PENDING, amo.getStatus());
        when(marketHoursService.isMarketOpen()).thenReturn(true);
        when(marketHoursService.isBeforeMarketOpen()).thenReturn(false);
        when(marketHoursService.isTodayTradingDay()).thenReturn(true);
        orderService.processDailyMarketOpenOrders();
        Order processed = orderRepository.findById(amo.getOrderId()).get();
        assertEquals(OrderStatus.FILLED, processed.getStatus());
    }
}


