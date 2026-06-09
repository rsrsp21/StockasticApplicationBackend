package com.stockasticappbackend.service.stock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.stock.StockRequest;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
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
import com.stockasticappbackend.util.Constants;

@SpringBootTest
class StockServiceImplTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Value("${file.upload-stocks:uploads/stocks}")
    private String stocksDir;

    @BeforeEach
    void setUp() {
        stockIndicatorRepository.deleteAll();
        autoSellRuleRepository.deleteAll();
        watchlistItemRepository.deleteAll();
        watchlistRepository.deleteAll();
        priceAlertRepository.deleteAll();
        sipRepository.deleteAll();
        orderRepository.deleteAll();
        holdingsRepository.deleteAll();
        stockPriceRepository.deleteAll();
        stockRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        try {
            Path path = Paths.get(stocksDir);
            if (Files.exists(path)) {
                Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("stock_"))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception e) {}
                    });
            }
        } catch (Exception e) {
        }
    }

    @Test
    void createStock_ShouldReturnStockResponse() {
        StockRequest request = new StockRequest();
        request.setSymbol("TEST1");
        request.setName("Test Stock 1");
        request.setExchange("NSE");
        request.setSector("Technology");
        request.setDescription("Test Description");
        request.setIsActive(true);
        StockResponse response = stockService.createStock(request, null);
        assertNotNull(response);
        assertEquals("TEST1", response.getSymbol());
        assertEquals("Test Stock 1", response.getName());
        
        Stock savedStock = stockRepository.findBySymbol("TEST1").orElse(null);
        assertNotNull(savedStock);
        assertEquals("Test Stock 1", savedStock.getName());
    }

    @Test
    void createStock_withImage_ShouldSaveImageFileName() {
        StockRequest request = new StockRequest();
        request.setSymbol("TEST_IMG");
        request.setName("Test Stock Image");
        request.setExchange("NSE");
        
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.webp",
                "image/webp",
                "test image content".getBytes()
        );
        StockResponse response = stockService.createStock(request, image);
        assertNotNull(response);
        assertTrue(response.getImage().startsWith("stock_"));
        assertTrue(response.getImage().endsWith(".webp"));
    }
    
    @Test
    void createStock_InvalidImageType_ShouldThrowException() {
        StockRequest request = new StockRequest();
        request.setSymbol("INV_IMG");
        request.setName("Invalid Image");
        request.setExchange("NSE");
        
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            stockService.createStock(request, image);
        });
        assertEquals(Constants.STOCK_IMAGE_TYPE_ERROR, exception.getMessage());
    }

    @Test
    void createStock_duplicateSymbol_ShouldThrowException() {
        StockRequest request = new StockRequest();
        request.setSymbol("DUPLICATE");
        request.setName("First");
        request.setExchange("NSE");
        stockService.createStock(request, null);

        StockRequest duplicateRequest = new StockRequest();
        duplicateRequest.setSymbol("DUPLICATE");
        duplicateRequest.setName("Second");
        duplicateRequest.setExchange("BSE");
        assertThrows(IllegalArgumentException.class, () -> {
            stockService.createStock(duplicateRequest, null);
        });
    }

    @Test
    void getStockById_ShouldReturnStock() {
        Stock stock = new Stock();
        stock.setSymbol("ID_TEST");
        stock.setName("ID Test");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stock = stockRepository.save(stock);
        StockResponse response = stockService.getStockById(stock.getStockId());
        assertNotNull(response);
        assertEquals("ID_TEST", response.getSymbol());
    }

    @Test
    void getStockById_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            stockService.getStockById(999999L);
        });
    }

    @Test
    void getStockBySymbol_ShouldReturnStock() {
        Stock stock = new Stock();
        stock.setSymbol("SYMB_TEST");
        stock.setName("Symbol Test");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stockRepository.save(stock);
        StockResponse response = stockService.getStockBySymbol("SYMB_TEST");
        assertNotNull(response);
        assertEquals("Symbol Test", response.getName());
    }

    @Test
    void getAllStocksPaged_ShouldReturnPage() {
        for (int i = 0; i < 5; i++) {
            Stock stock = new Stock();
            stock.setSymbol("PAGE_" + i);
            stock.setName("Page Test " + i);
            stock.setExchange("NSE");
            stock.setIsActive(true);
            stockRepository.save(stock);
        }
        PageResponse<StockResponse> response = stockService.getAllStocksPaged(0, 2, "symbol", "asc");
        assertEquals(2, response.getContent().size());
        assertTrue(response.getTotalElements() >= 5);
    }
    
    @Test
    void getActiveStocks_ShouldReturnOnlyActive() {
        Stock active = new Stock();
        active.setSymbol("ACTIVE");
        active.setName("Active Stock");
        active.setExchange("NSE");
        active.setIsActive(true);
        stockRepository.save(active);
        
        Stock inactive = new Stock();
        inactive.setSymbol("INACTIVE");
        inactive.setName("Inactive Stock");
        inactive.setExchange("NSE");
        inactive.setIsActive(false);
        stockRepository.save(inactive);
        List<StockResponse> responses = stockService.getActiveStocks();
        assertTrue(responses.stream().anyMatch(s -> s.getSymbol().equals("ACTIVE")));
        assertFalse(responses.stream().anyMatch(s -> s.getSymbol().equals("INACTIVE")));
    }

    @Test
    void toggleStockStatus_ShouldChangeIsActive() {
        Stock stock = new Stock();
        stock.setSymbol("TOGGLE");
        stock.setName("Toggle Test");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stock = stockRepository.save(stock);
        StockResponse response = stockService.toggleStockStatus(stock.getStockId());
        assertFalse(response.getIsActive());
        response = stockService.toggleStockStatus(stock.getStockId());
        assertTrue(response.getIsActive());
    }

    @Test
    void updateStock_ShouldUpdateDetails() {
        Stock stock = new Stock();
        stock.setSymbol("UPDATE");
        stock.setName("Old Name");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stock = stockRepository.save(stock);

        StockRequest updateRequest = new StockRequest();
        updateRequest.setSymbol("UPDATE");
        updateRequest.setName("New Name");
        updateRequest.setExchange("BSE");
        updateRequest.setIsActive(true);
        StockResponse response = stockService.updateStock(stock.getStockId(), updateRequest, null);
        assertEquals("New Name", response.getName());
        assertEquals("BSE", response.getExchange());
    }
    
    @Test
    void updateStock_WithImage_ShouldUpdateImage() {
        Stock stock = new Stock();
        stock.setSymbol("UPD_IMG");
        stock.setName("Update Image");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stock = stockRepository.save(stock);

        StockRequest updateRequest = new StockRequest();
        updateRequest.setSymbol("UPD_IMG");
        updateRequest.setName("Update Image");
        updateRequest.setExchange("NSE");
        
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "new.webp",
                "image/webp",
                "new content".getBytes()
        );
        StockResponse response = stockService.updateStock(stock.getStockId(), updateRequest, image);
        assertNotNull(response.getImage());
        assertTrue(response.getImage().startsWith("stock_" + stock.getStockId()));
    }

    @Test
    void deleteStock_ShouldRemoveFromDb() {
        Stock stock = new Stock();
        stock.setSymbol("DELETE");
        stock.setName("Delete Test");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stock = stockRepository.save(stock);
        Long id = stock.getStockId();
        stockService.deleteStock(id);
        assertFalse(stockRepository.findById(id).isPresent());
    }

    @Test
    void enrichWithLatestPrice_ShouldPopulatePriceData() {
        Stock stock = new Stock();
        stock.setSymbol("ENRICH");
        stock.setName("Enrich Test");
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stock = stockRepository.save(stock);

        StockPrice price = StockPrice.builder()
                .stock(stock)
                .price(new BigDecimal("150.0000"))
                .openPrice(new BigDecimal("145.0000"))
                .previousClose(new BigDecimal("140.0000"))
                .priceTime(LocalDateTime.now())
                .volume(1000L)
                .build();
        stockPriceRepository.save(price);
        StockResponse response = stockService.getStockById(stock.getStockId());
        assertEquals(0, new BigDecimal("150.0000").compareTo(response.getCurrentPrice()));
        assertNotNull(response.getChangePercent());
        assertEquals(0, new BigDecimal("7.1400").compareTo(response.getChangePercent()));
    }
    
    @Test
    void searchStocksWithFilters_ShouldReturnMatches() {
        Stock s1 = new Stock();
        s1.setSymbol("SEARCH1");
        s1.setName("Search Tech");
        s1.setExchange("NSE");
        s1.setSector("Technology");
        s1.setIsActive(true);
        stockRepository.save(s1);
        
        Stock s2 = new Stock();
        s2.setSymbol("SEARCH2");
        s2.setName("Search Pharma");
        s2.setExchange("BSE");
        s2.setSector("Pharma");
        s2.setIsActive(true);
        stockRepository.save(s2);
        PageResponse<StockResponse> resSector = stockService.searchStocksWithFilters(
                null, "Pharma", null, null, null, null, null, null, 0, 10, "symbol", "asc");
        
        assertEquals(1, resSector.getTotalElements());
        assertEquals("SEARCH2", resSector.getContent().get(0).getSymbol());
        PageResponse<StockResponse> resQuery = stockService.searchStocksWithFilters(
                "Tech", null, null, null, null, null, null, null, 0, 10, "symbol", "asc");
        assertEquals(1, resQuery.getTotalElements());
        assertEquals("SEARCH1", resQuery.getContent().get(0).getSymbol());
    }

    @Test
    void getAllStocks_ShouldReturnAll() {
        Stock s1 = new Stock();
        s1.setSymbol("ALL1");
        s1.setName("All 1");
        s1.setExchange("NSE");
        s1.setIsActive(true);
        stockRepository.save(s1);

        Stock s2 = new Stock();
        s2.setSymbol("ALL2");
        s2.setName("All 2");
        s2.setExchange("NSE");
        s2.setIsActive(false);
        stockRepository.save(s2);
        List<StockResponse> all = stockService.getAllStocks();
        assertTrue(all.size() >= 2);
    }

    @Test
    void getActiveStocksPaged_ShouldReturnPage() {
        Stock s1 = new Stock();
        s1.setSymbol("ACT1");
        s1.setName("Active 1");
        s1.setExchange("NSE");
        s1.setIsActive(true);
        stockRepository.save(s1);
        PageResponse<StockResponse> page = stockService.getActiveStocksPaged(0, 10, "symbol", "asc");
        assertTrue(page.getTotalElements() >= 1);
        assertTrue(page.getContent().stream().allMatch(StockResponse::getIsActive));
    }

    @Test
    void searchAllStocksWithFilters_ShouldReturnInactiveToo() {
        Stock s1 = new Stock();
        s1.setSymbol("S_ALL_1");
        s1.setName("Search All 1");
        s1.setExchange("NSE");
        s1.setIsActive(false);
        stockRepository.save(s1);
        PageResponse<StockResponse> page = stockService.searchAllStocksWithFilters(
                "S_ALL_1", null, null, null, null, null, null, null, 0, 10, "symbol", "asc");
        assertEquals(1, page.getTotalElements());
        assertEquals("S_ALL_1", page.getContent().get(0).getSymbol());
    }

    @Test
    void updateStock_DuplicateSymbol_ShouldThrowException() {
        Stock s1 = new Stock();
        s1.setSymbol("ORIG1");
        s1.setName("Original 1");
        s1.setExchange("NSE");
        s1.setIsActive(true);
        stockRepository.save(s1);

        Stock s2 = new Stock();
        s2.setSymbol("EXISTING");
        s2.setName("Existing");
        s2.setExchange("NSE");
        s2.setIsActive(true);
        stockRepository.save(s2);

        StockRequest update = new StockRequest();
        update.setSymbol("EXISTING");
        update.setName("New Name");
        update.setExchange("NSE");
        Long s1Id = s1.getStockId();
        assertThrows(IllegalArgumentException.class, () -> {
            stockService.updateStock(s1Id, update, null);
        });
    }

    @Test
    void deleteStock_WithImage_ShouldDeleteImageFile() throws Exception {
        Stock s1 = new Stock();
        s1.setSymbol("DEL_IMG");
        s1.setName("Delete Image");
        s1.setExchange("NSE");
        s1.setIsActive(true);
        s1.setImage("stock_delete_test.webp");
        s1 = stockRepository.save(s1);
        Path uploadPath = Paths.get(stocksDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve("stock_delete_test.webp");
        Files.write(filePath, "dummy content".getBytes());

        assertTrue(Files.exists(filePath));
        stockService.deleteStock(s1.getStockId());
        assertFalse(stockRepository.findById(s1.getStockId()).isPresent());
        assertFalse(Files.exists(filePath), "Image file should be deleted");
    }
}


