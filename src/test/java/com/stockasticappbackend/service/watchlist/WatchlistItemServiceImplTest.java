package com.stockasticappbackend.service.watchlist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.stockasticappbackend.dto.watchlist.AddStockToWatchlistRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistCreateRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistItemResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.exception.DuplicateResourceException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.AutoSellRuleRepository;
import com.stockasticappbackend.repository.HoldingsRepository;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.PriceAlertRepository;
import com.stockasticappbackend.repository.SipRepository;
import com.stockasticappbackend.repository.StockIndicatorRepository;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WatchlistItemRepository;
import com.stockasticappbackend.repository.WatchlistRepository;

@SpringBootTest
class WatchlistItemServiceImplTest {

    @Autowired
    private WatchlistItemService watchlistItemService;

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    private AppUser testUser;
    private Stock testStock;
    private Stock testStock2;
    private Long watchlistId;

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
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setEmail("witem@example.com");
        testUser.setName("WItem User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        testStock = new Stock();
        testStock.setSymbol("WITM_TST1");
        testStock.setName("Watchlist Item Test 1");
        testStock.setExchange("NSE");
        testStock.setIsActive(true);
        testStock = stockRepository.save(testStock);

        testStock2 = new Stock();
        testStock2.setSymbol("WITM_TST2");
        testStock2.setName("Watchlist Item Test 2");
        testStock2.setExchange("NSE");
        testStock2.setIsActive(true);
        testStock2 = stockRepository.save(testStock2);
        StockPrice price1 = StockPrice.builder()
                .stock(testStock)
                .price(new BigDecimal("150.0000"))
                .openPrice(new BigDecimal("148.0000"))
                .previousClose(new BigDecimal("145.0000"))
                .priceTime(LocalDateTime.now())
                .volume(5000L)
                .build();
        stockPriceRepository.save(price1);

        StockPrice price2 = StockPrice.builder()
                .stock(testStock2)
                .price(new BigDecimal("250.0000"))
                .openPrice(new BigDecimal("245.0000"))
                .previousClose(new BigDecimal("240.0000"))
                .priceTime(LocalDateTime.now())
                .volume(8000L)
                .build();
        stockPriceRepository.save(price2);
        WatchlistResponse wl = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Test Watchlist").build());
        watchlistId = wl.getId();
    }

    @Test
    void addStockToWatchlist_ShouldReturnResponse() {
        AddStockToWatchlistRequest request = AddStockToWatchlistRequest.builder()
                .stockId(testStock.getStockId())
                .build();
        WatchlistItemResponse response = watchlistItemService.addStockToWatchlist(
                watchlistId, testUser.getUserId(), request);
        assertNotNull(response);
        assertEquals(testStock.getStockId(), response.getStock().getStockId());
        assertEquals("WITM_TST1", response.getStock().getSymbol());
    }

    @Test
    void addStockToWatchlist_Duplicate_ShouldThrowException() {
        AddStockToWatchlistRequest request = AddStockToWatchlistRequest.builder()
                .stockId(testStock.getStockId())
                .build();
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(), request);

        AddStockToWatchlistRequest duplicate = AddStockToWatchlistRequest.builder()
                .stockId(testStock.getStockId())
                .build();
        assertThrows(DuplicateResourceException.class, () -> {
            watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(), duplicate);
        });
    }

    @Test
    void addStockToWatchlist_StockNotFound_ShouldThrowException() {
        AddStockToWatchlistRequest request = AddStockToWatchlistRequest.builder()
                .stockId(999999L)
                .build();
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(), request);
        });
    }

    @Test
    void addStockToWatchlist_WatchlistNotFound_ShouldThrowException() {
        AddStockToWatchlistRequest request = AddStockToWatchlistRequest.builder()
                .stockId(testStock.getStockId())
                .build();
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistItemService.addStockToWatchlist(999999L, testUser.getUserId(), request);
        });
    }

    @Test
    void removeStockFromWatchlist_ShouldRemoveSuccessfully() {
        AddStockToWatchlistRequest request = AddStockToWatchlistRequest.builder()
                .stockId(testStock.getStockId())
                .build();
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(), request);
        watchlistItemService.removeStockFromWatchlist(watchlistId, testUser.getUserId(), testStock.getStockId());
        List<WatchlistItemResponse> items = watchlistItemService.getWatchlistItemsWithPrices(
                watchlistId, testUser.getUserId());
        assertTrue(items.isEmpty());
    }

    @Test
    void removeStockFromWatchlist_NotInWatchlist_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistItemService.removeStockFromWatchlist(
                    watchlistId, testUser.getUserId(), testStock.getStockId());
        });
    }

    @Test
    void getWatchlistItemsWithPrices_ShouldReturnItems() {
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(),
                AddStockToWatchlistRequest.builder().stockId(testStock.getStockId()).build());
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(),
                AddStockToWatchlistRequest.builder().stockId(testStock2.getStockId()).build());
        List<WatchlistItemResponse> items = watchlistItemService.getWatchlistItemsWithPrices(
                watchlistId, testUser.getUserId());
        assertEquals(2, items.size());
    }

    @Test
    void getWatchlistItemsWithPrices_EmptyWatchlist_ShouldReturnEmptyList() {
        List<WatchlistItemResponse> items = watchlistItemService.getWatchlistItemsWithPrices(
                watchlistId, testUser.getUserId());
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void isStockInUserWatchlists_Exists_ShouldReturnTrue() {
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(),
                AddStockToWatchlistRequest.builder().stockId(testStock.getStockId()).build());
        boolean result = watchlistItemService.isStockInUserWatchlists(
                testStock.getStockId(), testUser.getUserId());
        assertTrue(result);
    }

    @Test
    void isStockInUserWatchlists_NotExists_ShouldReturnFalse() {
        boolean result = watchlistItemService.isStockInUserWatchlists(
                testStock.getStockId(), testUser.getUserId());
        assertFalse(result);
    }

    @Test
    void getWatchlistsContainingStock_ShouldReturnWatchlists() {
        WatchlistResponse wl2 = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Second List").build());
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(),
                AddStockToWatchlistRequest.builder().stockId(testStock.getStockId()).build());
        watchlistItemService.addStockToWatchlist(wl2.getId(), testUser.getUserId(),
                AddStockToWatchlistRequest.builder().stockId(testStock.getStockId()).build());
        List<WatchlistResponse> watchlists = watchlistItemService.getWatchlistsContainingStock(
                testStock.getStockId(), testUser.getUserId());
        assertEquals(2, watchlists.size());
    }

    @Test
    void getWatchlistIdsContainingStock_ShouldReturnIds() {
        watchlistItemService.addStockToWatchlist(watchlistId, testUser.getUserId(),
                AddStockToWatchlistRequest.builder().stockId(testStock.getStockId()).build());
        List<Long> ids = watchlistItemService.getWatchlistIdsContainingStock(
                testStock.getStockId(), testUser.getUserId());
        assertEquals(1, ids.size());
        assertTrue(ids.contains(watchlistId));
    }

    @Test
    void addStockToWatchlist_WrongUser_ShouldThrowException() {
        AppUser otherUser = new AppUser();
        otherUser.setEmail("other_witem@example.com");
        otherUser.setName("Other");
        otherUser.setPasswordHash("hash");
        otherUser = userRepository.save(otherUser);

        AddStockToWatchlistRequest request = AddStockToWatchlistRequest.builder()
                .stockId(testStock.getStockId())
                .build();

        Long otherUserId = otherUser.getUserId();
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistItemService.addStockToWatchlist(watchlistId, otherUserId, request);
        });
    }
}

