package com.stockasticappbackend.service.sip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.stockasticappbackend.config.TestConfig;
import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.dto.sip.SipRequest;
import com.stockasticappbackend.dto.sip.SipResponse;
import com.stockasticappbackend.dto.sip.SipTransactionResponse;
import com.stockasticappbackend.exception.InsufficientFundsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.entity.Sip;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.SipFrequency;
import com.stockasticappbackend.model.enums.SipStatus;
import com.stockasticappbackend.model.enums.SipTransactionStatus;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.HoldingsRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.PriceAlertRepository;
import com.stockasticappbackend.repository.SipRepository;
import com.stockasticappbackend.repository.SipTransactionRepository;
import com.stockasticappbackend.repository.StockIndicatorRepository;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WalletRepository;
import com.stockasticappbackend.repository.WatchlistItemRepository;
import com.stockasticappbackend.repository.WatchlistRepository;
import com.stockasticappbackend.service.notification.NotificationService;
import com.stockasticappbackend.service.order.OrderService;
import com.stockasticappbackend.service.stockprice.MarketHoursService;

@SpringBootTest
@Import(TestConfig.class)
class SipServiceImplTest {

    @Autowired
    private SipServiceImpl sipService;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private SipTransactionRepository sipTransactionRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private KycRepository kycRepository;
    
    @Autowired
    private StockPriceRepository stockPriceRepository;
    
    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @MockitoBean
    private MarketHoursService marketHoursService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private NotificationService notificationService;

    private AppUser testUser;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        stockIndicatorRepository.deleteAll();
        watchlistItemRepository.deleteAll();
        watchlistRepository.deleteAll();
        priceAlertRepository.deleteAll();
        autoSellRuleRepository.deleteAll();
        sipTransactionRepository.deleteAll();
        sipRepository.deleteAll();
        orderRepository.deleteAll();
        holdingsRepository.deleteAll();
        stockPriceRepository.deleteAll();
        kycRepository.deleteAll();
        walletRepository.deleteAll();
        stockRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setEmail("sip@example.com");
        testUser.setName("SIP User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.saveAndFlush(testUser);
        Kyc kyc = new Kyc();
        kyc.setUser(testUser);
        kyc.setKycStatus(KycStatus.APPROVED);
        kyc.setPanNumber("SIPAB1234C");
        kyc.setAadhaarNumber("123456789012");
        kyc.setDocumentPath("test_path");
        kycRepository.saveAndFlush(kyc);
        Wallet wallet = new Wallet();
        wallet.setUser(testUser);
        wallet.setAvailableBalance(new BigDecimal("500000.00"));
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR");
        walletRepository.saveAndFlush(wallet);

        testStock = new Stock();
        testStock.setSymbol("SIP_TEST");
        testStock.setName("SIP Test Stock");
        testStock.setExchange("NSE");
        testStock.setIsActive(true);
        testStock = stockRepository.saveAndFlush(testStock);

        StockPrice price = StockPrice.builder()
                .stock(testStock)
                .price(new BigDecimal("500.0000"))
                .openPrice(new BigDecimal("495.0000"))
                .previousClose(new BigDecimal("490.0000"))
                .priceTime(LocalDateTime.now())
                .volume(10000L)
                .build();
        stockPriceRepository.saveAndFlush(price);
        when(marketHoursService.isTradingDay(any())).thenReturn(true);
    }

    @Test
    void createSip_Monthly_ShouldReturnResponse() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse response = sipService.createSip("sip@example.com", request);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(SipFrequency.MONTHLY, response.getFrequency());
        assertEquals(5, response.getQuantity());
        assertEquals(SipStatus.ACTIVE, response.getStatus());
        verify(notificationService).createNotification(any(AppUser.class), anyString(), anyString(), any());
    }

    @Test
    void createSip_Yearly_ShouldReturnResponse() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.YEARLY);
        request.setQuantity(10);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse response = sipService.createSip("sip@example.com", request);
        assertNotNull(response);
        assertEquals(SipFrequency.YEARLY, response.getFrequency());
    }

    @Test
    void createSip_StockNotFound_ShouldThrowException() {
        SipRequest request = new SipRequest();
        request.setStockId(999999L);
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.createSip("sip@example.com", request);
        });
    }

    @Test
    void createSip_UserNotFound_ShouldThrowException() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.createSip("nonexistent@example.com", request);
        });
    }

    @Test
    void getSip_ShouldReturnSip() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse created = sipService.createSip("sip@example.com", request);
        SipResponse retrieved = sipService.getSip("sip@example.com", created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void getSip_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.getSip("sip@example.com", 999999L);
        });
    }

    @Test
    void getUserSips_ShouldReturnAll() {
        SipRequest r1 = new SipRequest();
        r1.setStockId(testStock.getStockId());
        r1.setFrequency(SipFrequency.MONTHLY);
        r1.setQuantity(5);
        r1.setStartDate(LocalDate.now().plusDays(1));
        sipService.createSip("sip@example.com", r1);
        Stock stock2 = new Stock();
        stock2.setSymbol("SIP_TST2");
        stock2.setName("SIP Test 2");
        stock2.setExchange("NSE");
        stock2.setIsActive(true);
        stock2 = stockRepository.saveAndFlush(stock2);

        StockPrice price2 = StockPrice.builder()
                .stock(stock2)
                .price(new BigDecimal("300.0000"))
                .openPrice(new BigDecimal("295.0000"))
                .previousClose(new BigDecimal("290.0000"))
                .priceTime(LocalDateTime.now())
                .volume(5000L)
                .build();
        stockPriceRepository.saveAndFlush(price2);

        SipRequest r2 = new SipRequest();
        r2.setStockId(stock2.getStockId());
        r2.setFrequency(SipFrequency.YEARLY);
        r2.setQuantity(10);
        r2.setStartDate(LocalDate.now().plusDays(1));
        sipService.createSip("sip@example.com", r2);
        List<SipResponse> sips = sipService.getUserSips("sip@example.com");
        assertEquals(2, sips.size());
    }

    @Test
    void getUserSips_Empty_ShouldReturnEmptyList() {
        List<SipResponse> sips = sipService.getUserSips("sip@example.com");
        assertNotNull(sips);
        assertTrue(sips.isEmpty());
    }

    @Test
    void toggleSipStatus_PauseSip_ShouldChangeToPaused() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse created = sipService.createSip("sip@example.com", request);
        SipResponse paused = sipService.toggleSipStatus("sip@example.com",
                created.getId(), SipStatus.PAUSED);
        assertEquals(SipStatus.PAUSED, paused.getStatus());
    }

    @Test
    void toggleSipStatus_ResumeSip_ShouldChangeToActive() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().minusDays(10));
        SipResponse created = sipService.createSip("sip@example.com", request);
        sipService.toggleSipStatus("sip@example.com", created.getId(), SipStatus.PAUSED);
        SipResponse resumed = sipService.toggleSipStatus("sip@example.com",
                created.getId(), SipStatus.ACTIVE);
        assertEquals(SipStatus.ACTIVE, resumed.getStatus());
    }

    @Test
    void toggleSipStatus_CancelSip_ShouldChangeToCancelled() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse created = sipService.createSip("sip@example.com", request);
        SipResponse cancelled = sipService.toggleSipStatus("sip@example.com",
                created.getId(), SipStatus.CANCELLED);
        assertEquals(SipStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void updateSip_ShouldUpdateQuantity() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse created = sipService.createSip("sip@example.com", request);

        SipRequest update = new SipRequest();
        update.setStockId(testStock.getStockId());
        update.setFrequency(SipFrequency.MONTHLY);
        update.setQuantity(10);
        update.setStartDate(LocalDate.now().plusDays(1));
        SipResponse updated = sipService.updateSip("sip@example.com", created.getId(), update);
        assertEquals(10, updated.getQuantity());
    }

    @Test
    void updateSip_NotFound_ShouldThrowException() {
        SipRequest update = new SipRequest();
        update.setStockId(testStock.getStockId());
        update.setFrequency(SipFrequency.MONTHLY);
        update.setQuantity(10);
        update.setStartDate(LocalDate.now().plusDays(1));
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.updateSip("sip@example.com", 999999L, update);
        });
    }

    @Test
    void getSipsByStock_ShouldReturnSipsForStock() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        sipService.createSip("sip@example.com", request);
        List<SipResponse> sips = sipService.getSipsByStock("sip@example.com", testStock.getStockId());
        assertEquals(1, sips.size());
    }

    @Test
    void getSipsByStock_NoSips_ShouldReturnEmpty() {
        List<SipResponse> sips = sipService.getSipsByStock("sip@example.com", testStock.getStockId());
        assertNotNull(sips);
        assertTrue(sips.isEmpty());
    }

    @Test
    void getSipHistory_Empty_ShouldReturnEmptyPage() {
        Page<SipTransactionResponse> history = sipService.getSipHistory(
                "sip@example.com", PageRequest.of(0, 10));
        assertNotNull(history);
        assertEquals(0, history.getTotalElements());
    }

    @Test
    void getSip_WrongUser_ShouldThrowException() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse created = sipService.createSip("sip@example.com", request);
        AppUser otherUser = new AppUser();
        otherUser.setEmail("other_sip@example.com");
        otherUser.setName("Other");
        otherUser.setPasswordHash("hash");
        otherUser.setUserStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(otherUser);

        Long sipId = created.getId();
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.getSip("other_sip@example.com", sipId);
        });
    }

    @Test
    void processDailySips_InsufficientFunds_ShouldSkip() {
        Sip sip = new Sip();
        sip.setUser(testUser);
        sip.setStock(testStock);
        sip.setFrequency(SipFrequency.MONTHLY);
        sip.setQuantity(5);
        sip.setStartDate(LocalDate.now());
        sip.setNextExecutionDate(LocalDate.now());
        sip.setStatus(SipStatus.ACTIVE);
        sipRepository.saveAndFlush(sip);
        when(marketHoursService.isTradingDay(any())).thenReturn(true);

        when(orderService.placeOrder(anyString(), any(OrderRequest.class)))
                .thenThrow(new InsufficientFundsException("No money"));
        sipService.processDailySips(false);
        List<com.stockasticappbackend.model.entity.SipTransaction> txs = sipTransactionRepository.findAll();
        assertEquals(1, txs.size());
        assertEquals(SipTransactionStatus.SKIPPED_INSUFFICIENT_FUNDS, txs.get(0).getStatus());
        Sip updatedSip = sipRepository.findById(sip.getId()).get();
        assertEquals(LocalDate.now().plusMonths(1), updatedSip.getNextExecutionDate());
    }

    @Test
    void processDailySips_Holiday_ShouldPostpone() {
        when(marketHoursService.isTradingDay(any())).thenReturn(false);

        Sip sip = new Sip();
        sip.setUser(testUser);
        sip.setStock(testStock);
        sip.setFrequency(SipFrequency.MONTHLY);
        sip.setQuantity(5);
        sip.setStartDate(LocalDate.now());
        sip.setNextExecutionDate(LocalDate.now());
        sip.setStatus(SipStatus.ACTIVE);
        sipRepository.saveAndFlush(sip);
        sipService.processDailySips(false);
        Sip updatedSip = sipRepository.findById(sip.getId()).get();
        assertEquals(LocalDate.now().plusDays(1), updatedSip.getNextExecutionDate());
        verify(orderService, never()).placeOrder(anyString(), any());
    }

    @Test
    void processDailySips_OrderFailure_ShouldLogFailure() {
        Sip sip = new Sip();
        sip.setUser(testUser);
        sip.setStock(testStock);
        sip.setFrequency(SipFrequency.MONTHLY);
        sip.setQuantity(5);
        sip.setStartDate(LocalDate.now());
        sip.setNextExecutionDate(LocalDate.now());
        sip.setStatus(SipStatus.ACTIVE);
        sipRepository.saveAndFlush(sip);
        when(marketHoursService.isTradingDay(any())).thenReturn(true);

        when(orderService.placeOrder(anyString(), any(OrderRequest.class)))
                .thenThrow(new RuntimeException("Order Gateway Down"));
        sipService.processDailySips(false);
        List<com.stockasticappbackend.model.entity.SipTransaction> txs = sipTransactionRepository.findAll();
        assertEquals(1, txs.size());
        assertEquals(SipTransactionStatus.FAILED, txs.get(0).getStatus());
        Sip updatedSip = sipRepository.findById(sip.getId()).get();
        assertEquals(LocalDate.now().plusMonths(1), updatedSip.getNextExecutionDate());
    }

    @Test
    void processDailySips_SuccessfulExecution_ShouldLogSuccess() {
        Sip sip = new Sip();
        sip.setUser(testUser);
        sip.setStock(testStock);
        sip.setFrequency(SipFrequency.MONTHLY);
        sip.setQuantity(2);
        sip.setStartDate(LocalDate.now());
        sip.setNextExecutionDate(LocalDate.now());
        sip.setStatus(SipStatus.ACTIVE);
        sipRepository.saveAndFlush(sip);

        when(marketHoursService.isTradingDay(any())).thenReturn(true);
        OrderResponse orderResp = new OrderResponse();
        orderResp.setOrderId(999L);
        when(orderService.placeOrder(anyString(), any(OrderRequest.class))).thenReturn(orderResp);
        sipService.processDailySips(false);
        List<com.stockasticappbackend.model.entity.SipTransaction> txs = sipTransactionRepository.findAll();
        assertEquals(1, txs.size());
        assertEquals(SipTransactionStatus.SUCCESS, txs.get(0).getStatus());
        Sip updatedSip = sipRepository.findById(sip.getId()).get();
        assertEquals(LocalDate.now().plusMonths(1), updatedSip.getNextExecutionDate());
    }

    @Test
    void processDailySips_WithNotifyUpcoming_ShouldNotifyTomorrowSips() {
        Sip sip = new Sip();
        sip.setUser(testUser);
        sip.setStock(testStock);
        sip.setFrequency(SipFrequency.MONTHLY);
        sip.setQuantity(3);
        sip.setStartDate(LocalDate.now().plusDays(1));
        sip.setNextExecutionDate(LocalDate.now().plusDays(1));
        sip.setStatus(SipStatus.ACTIVE);
        sipRepository.saveAndFlush(sip);

        when(marketHoursService.isTradingDay(any())).thenReturn(true);
        sipService.processDailySips(true);
        verify(notificationService).createNotification(any(AppUser.class), 
                eq("Upcoming SIP"), anyString(), any());
    }

    @Test
    void processDailySips_NoDueSips_ShouldDoNothing() {
        when(marketHoursService.isTradingDay(any())).thenReturn(true);
        sipService.processDailySips(false);
        verify(orderService, never()).placeOrder(anyString(), any());
        assertTrue(sipTransactionRepository.findAll().isEmpty());
    }

    @Test
    void updateSip_StartDateChangedToFuture_ShouldResetNextDate() {
        SipRequest createReq = new SipRequest();
        createReq.setStockId(testStock.getStockId());
        createReq.setFrequency(SipFrequency.MONTHLY);
        createReq.setQuantity(5);
        createReq.setStartDate(LocalDate.now().minusDays(10));
        SipResponse created = sipService.createSip("sip@example.com", createReq);
        SipRequest updateReq = new SipRequest();
        updateReq.setStockId(testStock.getStockId());
        updateReq.setFrequency(SipFrequency.MONTHLY);
        updateReq.setQuantity(7);
        updateReq.setStartDate(LocalDate.now().plusDays(30));
        SipResponse updated = sipService.updateSip("sip@example.com", created.getId(), updateReq);
        assertEquals(7, updated.getQuantity());
        Sip updatedSip = sipRepository.findById(created.getId()).get();
        assertTrue(!updatedSip.getNextExecutionDate().isBefore(LocalDate.now()));
        verify(notificationService).createNotification(any(AppUser.class), 
                eq("SIP Updated"), anyString(), any());
    }

    @Test
    void updateSip_StartDateChangedToPast_ShouldRecalculateNext() {
        SipRequest createReq = new SipRequest();
        createReq.setStockId(testStock.getStockId());
        createReq.setFrequency(SipFrequency.MONTHLY);
        createReq.setQuantity(5);
        createReq.setStartDate(LocalDate.now().plusDays(15));
        SipResponse created = sipService.createSip("sip@example.com", createReq);
        SipRequest updateReq = new SipRequest();
        updateReq.setStockId(testStock.getStockId());
        updateReq.setFrequency(SipFrequency.MONTHLY);
        updateReq.setQuantity(3);
        updateReq.setStartDate(LocalDate.now().minusDays(5));
        SipResponse updated = sipService.updateSip("sip@example.com", created.getId(), updateReq);
        assertEquals(3, updated.getQuantity());
        Sip updatedSip = sipRepository.findById(created.getId()).get();
        assertTrue(updatedSip.getNextExecutionDate().isAfter(LocalDate.now()));
    }

    @Test
    void updateSip_SameStartDate_ShouldOnlyUpdateQuantity() {
        LocalDate startDate = LocalDate.now().plusDays(5);
        SipRequest createReq = new SipRequest();
        createReq.setStockId(testStock.getStockId());
        createReq.setFrequency(SipFrequency.MONTHLY);
        createReq.setQuantity(5);
        createReq.setStartDate(startDate);
        SipResponse created = sipService.createSip("sip@example.com", createReq);
        SipRequest updateReq = new SipRequest();
        updateReq.setStockId(testStock.getStockId());
        updateReq.setFrequency(SipFrequency.MONTHLY);
        updateReq.setQuantity(20);
        updateReq.setStartDate(startDate);
        SipResponse updated = sipService.updateSip("sip@example.com", created.getId(), updateReq);
        assertEquals(20, updated.getQuantity());
    }

    @Test
    void getUserSips_UserNotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.getUserSips("ghost@example.com");
        });
    }

    @Test
    void getSipsByStock_UserNotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.getSipsByStock("ghost@example.com", testStock.getStockId());
        });
    }

    @Test
    void getSipHistory_UserNotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            sipService.getSipHistory("ghost@example.com", PageRequest.of(0, 10));
        });
    }

    @Test
    void processDailySips_YearlySip_ShouldScheduleNextYear() {
        Sip sip = new Sip();
        sip.setUser(testUser);
        sip.setStock(testStock);
        sip.setFrequency(SipFrequency.YEARLY);
        sip.setQuantity(10);
        sip.setStartDate(LocalDate.now());
        sip.setNextExecutionDate(LocalDate.now());
        sip.setStatus(SipStatus.ACTIVE);
        sipRepository.saveAndFlush(sip);

        when(marketHoursService.isTradingDay(any())).thenReturn(true);
        when(orderService.placeOrder(anyString(), any(OrderRequest.class)))
                .thenThrow(new InsufficientFundsException("No funds"));
        sipService.processDailySips(false);
        Sip updatedSip = sipRepository.findById(sip.getId()).get();
        assertEquals(LocalDate.now().plusYears(1), updatedSip.getNextExecutionDate());

        List<com.stockasticappbackend.model.entity.SipTransaction> txs = sipTransactionRepository.findAll();
        assertEquals(1, txs.size());
        assertEquals(SipTransactionStatus.SKIPPED_INSUFFICIENT_FUNDS, txs.get(0).getStatus());
    }

    @Test
    void toggleSipStatus_ActiveToActive_ShouldStayActive() {
        SipRequest request = new SipRequest();
        request.setStockId(testStock.getStockId());
        request.setFrequency(SipFrequency.MONTHLY);
        request.setQuantity(5);
        request.setStartDate(LocalDate.now().plusDays(1));
        SipResponse created = sipService.createSip("sip@example.com", request);
        SipResponse result = sipService.toggleSipStatus("sip@example.com",
                created.getId(), SipStatus.ACTIVE);
        assertEquals(SipStatus.ACTIVE, result.getStatus());
    }
}


