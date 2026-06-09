package com.stockasticappbackend.service.wallet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Order;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.entity.WalletTransaction;
import com.stockasticappbackend.model.enums.MarketSession;
import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderStatus;
import com.stockasticappbackend.model.enums.OrderType;
import com.stockasticappbackend.model.enums.TransactionStatus;
import com.stockasticappbackend.model.enums.TransactionType;
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
class FundSettlementServiceTest {

    @Autowired
    private FundSettlementService fundSettlementService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

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
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private AppUser testUser;
    private Stock testStock;
    private Wallet testWallet;

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
        testUser.setEmail("settlement@test.com");
        testUser.setName("Settlement User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        testStock = new Stock();
        testStock.setSymbol("RELIANCE");
        testStock.setName("Reliance Industries");
        testStock.setExchange("NSE");
        testStock.setSector("Energy");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);

        testWallet = new Wallet();
        testWallet.setUser(testUser);
        testWallet.setAvailableBalance(new BigDecimal("50000.00"));
        testWallet.setLockedBalance(BigDecimal.ZERO);
        testWallet = walletRepository.save(testWallet);
    }

    /**
     * Helper to create a FILLED SELL order with given executed time.
     */
    private Order createSellOrder(BigDecimal filledPrice, int filledQty, BigDecimal brokerage,
                                   LocalDateTime executedAt) {
        Order order = Order.builder()
                .user(testUser)
                .stock(testStock)
                .orderType(OrderType.SELL)
                .orderMode(OrderMode.MARKET)
                .status(OrderStatus.FILLED)
                .marketSession(MarketSession.MARKET_HOURS)
                .isAmo(false)
                .quantity(filledQty)
                .filledQuantity(filledQty)
                .price(filledPrice)
                .averageFilledPrice(filledPrice)
                .totalAmount(filledPrice.multiply(BigDecimal.valueOf(filledQty)))
                .brokerage(brokerage)
                .isSettled(false)
                .executedAt(executedAt)
                .build();
        return orderRepository.save(order);
    }

    @Test
    void processSettlements_NoEligibleOrders_ShouldReturnZero() {
        int settled = fundSettlementService.processSettlements();
        assertEquals(0, settled);
    }

    @Test
    void processSettlements_OrderTooRecent_ShouldNotSettle() {
        createSellOrder(new BigDecimal("2500.00"), 10, new BigDecimal("20.00"),
                LocalDateTime.now());
        testWallet.setLockedBalance(new BigDecimal("24980.00"));
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(0, settled);
    }

    @Test
    void processSettlements_EligibleOrder_ShouldSettleFunds() {
        BigDecimal filledPrice = new BigDecimal("2500.00");
        int qty = 10;
        BigDecimal brokerage = new BigDecimal("20.00");
        BigDecimal netAmount = filledPrice.multiply(BigDecimal.valueOf(qty)).subtract(brokerage);

        createSellOrder(filledPrice, qty, brokerage, LocalDateTime.now().minusHours(25));
        testWallet.setLockedBalance(netAmount);
        testWallet.setAvailableBalance(new BigDecimal("50000.00"));
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(1, settled);
        Wallet updatedWallet = walletRepository.findById(testWallet.getWalletId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet.getLockedBalance()));
        assertEquals(0, new BigDecimal("50000.00").add(netAmount).compareTo(updatedWallet.getAvailableBalance()));
    }

    @Test
    void processSettlements_MultipleEligibleOrders_ShouldSettleAll() {
        BigDecimal brokerage = new BigDecimal("10.00");
        createSellOrder(new BigDecimal("2500.00"), 5, brokerage,
                LocalDateTime.now().minusHours(30));
        createSellOrder(new BigDecimal("3000.00"), 3, brokerage,
                LocalDateTime.now().minusHours(28));

        BigDecimal totalLocked = new BigDecimal("12490.00").add(new BigDecimal("8990.00"));
        testWallet.setLockedBalance(totalLocked);
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(2, settled);
    }

    @Test
    void processSettlements_PartialLockedBalance_ShouldMoveOnlyAvailable() {
        BigDecimal filledPrice = new BigDecimal("2500.00");
        int qty = 10;
        BigDecimal brokerage = new BigDecimal("20.00");

        createSellOrder(filledPrice, qty, brokerage, LocalDateTime.now().minusHours(25));
        testWallet.setLockedBalance(new BigDecimal("10000.00"));
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(1, settled);

        Wallet updatedWallet = walletRepository.findById(testWallet.getWalletId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet.getLockedBalance()));
        assertEquals(0, new BigDecimal("60000.00").compareTo(updatedWallet.getAvailableBalance()));
    }

    @Test
    void processSettlements_ZeroLockedBalance_ShouldMarkSettledWithoutMovingFunds() {
        createSellOrder(new BigDecimal("2500.00"), 10, new BigDecimal("20.00"),
                LocalDateTime.now().minusHours(25));
        testWallet.setLockedBalance(BigDecimal.ZERO);
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(1, settled);
        var orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        assertTrue(orders.get(0).getIsSettled());
        assertNotNull(orders.get(0).getSettledAt());
        Wallet updatedWallet = walletRepository.findById(testWallet.getWalletId()).orElseThrow();
        assertEquals(0, new BigDecimal("50000.00").compareTo(updatedWallet.getAvailableBalance()));
    }

    @Test
    void processSettlements_ShouldCreateTransactionRecord() {
        BigDecimal filledPrice = new BigDecimal("2000.00");
        int qty = 5;
        BigDecimal brokerage = BigDecimal.ZERO;
        BigDecimal netAmount = filledPrice.multiply(BigDecimal.valueOf(qty));

        createSellOrder(filledPrice, qty, brokerage, LocalDateTime.now().minusHours(25));

        testWallet.setLockedBalance(netAmount);
        walletRepository.save(testWallet);

        fundSettlementService.processSettlements();
        var transactions = transactionRepository.findAll();
        boolean hasSettlement = transactions.stream()
                .anyMatch(t -> t.getReferenceId() != null && t.getReferenceId().startsWith("SETTLE-"));
        assertTrue(hasSettlement);
    }

    @Test
    void processSettlements_ShouldSetIsSettledAndTimestamp() {
        createSellOrder(new BigDecimal("2000.00"), 5, BigDecimal.ZERO,
                LocalDateTime.now().minusHours(25));

        testWallet.setLockedBalance(new BigDecimal("10000.00"));
        walletRepository.save(testWallet);

        fundSettlementService.processSettlements();
        var orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        assertTrue(orders.get(0).getIsSettled());
        assertNotNull(orders.get(0).getSettledAt());
    }

    @Test
    void processSettlements_NullBrokerage_ShouldTreatAsZero() {
        BigDecimal filledPrice = new BigDecimal("1000.00");
        int qty = 10;
        BigDecimal netAmount = filledPrice.multiply(BigDecimal.valueOf(qty));

        createSellOrder(filledPrice, qty, null, LocalDateTime.now().minusHours(25));

        testWallet.setLockedBalance(netAmount);
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(1, settled);

        Wallet updatedWallet = walletRepository.findById(testWallet.getWalletId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedWallet.getLockedBalance()));
        assertEquals(0, new BigDecimal("60000.00").compareTo(updatedWallet.getAvailableBalance()));
    }

    @Test
    void processSettlements_AlreadySettledOrder_ShouldNotResettle() {
        Order order = createSellOrder(new BigDecimal("2000.00"), 5, BigDecimal.ZERO,
                LocalDateTime.now().minusHours(25));
        order.setIsSettled(true);
        order.setSettledAt(LocalDateTime.now().minusHours(1));
        orderRepository.save(order);

        testWallet.setLockedBalance(new BigDecimal("10000.00"));
        walletRepository.save(testWallet);

        int settled = fundSettlementService.processSettlements();
        assertEquals(0, settled);
    }

    @Test
    void processSettlements_ExistingSettlementReference_ShouldRemainIdempotent() {
        Order order = createSellOrder(new BigDecimal("2000.00"), 5, BigDecimal.ZERO,
                LocalDateTime.now().minusHours(25));
        testWallet.setLockedBalance(new BigDecimal("10000.00"));
        walletRepository.save(testWallet);

        WalletTransaction existing = new WalletTransaction();
        existing.setWallet(testWallet);
        existing.setAmount(new BigDecimal("10000.00"));
        existing.setType(TransactionType.CREDIT);
        existing.setStatus(TransactionStatus.SUCCESS);
        existing.setReferenceId("SETTLE-" + order.getOrderId());
        existing.setDescription("Existing settlement record");
        transactionRepository.save(existing);

        int settled = fundSettlementService.processSettlements();
        assertEquals(1, settled);

        long sameRefCount = transactionRepository.findAll().stream()
                .filter(t -> ("SETTLE-" + order.getOrderId()).equals(t.getReferenceId()))
                .count();
        assertEquals(1, sameRefCount);
    }
}


