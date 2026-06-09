package com.stockasticappbackend.service.admin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.stockasticappbackend.config.TestConfig;
import com.stockasticappbackend.dto.admin.AdminDashboardStatsResponse;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.UserRole;
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
import com.stockasticappbackend.repository.WatchlistItemRepository;
import com.stockasticappbackend.repository.WatchlistRepository;

@SpringBootTest
@Import(TestConfig.class)
class AdminDashboardServiceImplTest {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private KycRepository kycRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingsRepository holdingsRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SipRepository sipRepository;

    @Autowired
    private SipTransactionRepository sipTransactionRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private WatchlistItemRepository watchlistItemRepository;

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private StockIndicatorRepository stockIndicatorRepository;

    @Autowired
    private StockPriceRepository stockPriceRepository;

    @Autowired
    private AutoSellRuleRepository autoSellRuleRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

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
        stockRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getDashboardStats_EmptySystem_ShouldReturnZeros() {
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        assertNotNull(stats);
        assertEquals(0, stats.getTotalUsers());
        assertEquals(0, stats.getKycApproved());
        assertEquals(0, stats.getKycRejected());
        assertEquals(0, stats.getKycPending());
        assertEquals(0, stats.getTotalStocks());
    }

    @Test
    void getDashboardStats_WithData_ShouldReturnCorrectCounts() {
        AppUser user1 = createUser("user1@example.com", "User 1", UserRole.USER);
        AppUser user2 = createUser("user2@example.com", "User 2", UserRole.USER);
        AppUser user3 = createUser("user3@example.com", "User 3", UserRole.USER);
        createUser("admin@example.com", "Admin", UserRole.ADMIN);
        createKyc(user1, KycStatus.APPROVED);
        createKyc(user2, KycStatus.REJECTED);
        createKyc(user3, KycStatus.PENDING);
        createStock("STOCK1", "Stock 1");
        createStock("STOCK2", "Stock 2");
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        assertEquals(3, stats.getTotalUsers());
        assertEquals(1, stats.getKycApproved());
        assertEquals(1, stats.getKycRejected());
        assertEquals(1, stats.getKycPending());
        assertEquals(2, stats.getTotalStocks());
    }

    @Test
    void getDashboardStats_OnlyAdmins_ShouldShowZeroUsers() {
        createUser("admin1@example.com", "Admin 1", UserRole.ADMIN);
        createUser("admin2@example.com", "Admin 2", UserRole.ADMIN);
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        assertEquals(0, stats.getTotalUsers());
    }

    @Test
    void getDashboardStats_MultipleKycStatuses_ShouldCountCorrectly() {
        for (int i = 0; i < 5; i++) {
            AppUser user = createUser("approved" + i + "@example.com", "Approved " + i, UserRole.USER);
            createKyc(user, KycStatus.APPROVED);
        }
        for (int i = 0; i < 3; i++) {
            AppUser user = createUser("pending" + i + "@example.com", "Pending " + i, UserRole.USER);
            createKyc(user, KycStatus.PENDING);
        }
        AppUser rejectedUser = createUser("rejected@example.com", "Rejected", UserRole.USER);
        createKyc(rejectedUser, KycStatus.REJECTED);
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        assertEquals(9, stats.getTotalUsers());
        assertEquals(5, stats.getKycApproved());
        assertEquals(3, stats.getKycPending());
        assertEquals(1, stats.getKycRejected());
    }
    private AppUser createUser(String email, String name, UserRole role) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return userRepository.save(user);
    }

    private void createKyc(AppUser user, KycStatus status) {
        Kyc kyc = new Kyc();
        kyc.setUser(user);
        kyc.setKycStatus(status);
        kyc.setPanNumber("PAN" + user.getUserId());
        kyc.setAadhaarNumber("AADH" + user.getUserId());
        kyc.setDocumentPath("test_path");
        kycRepository.save(kyc);
    }

    private void createStock(String symbol, String name) {
        Stock stock = new Stock();
        stock.setSymbol(symbol);
        stock.setName(name);
        stock.setExchange("NSE");
        stock.setIsActive(true);
        stockRepository.save(stock);
    }
}


