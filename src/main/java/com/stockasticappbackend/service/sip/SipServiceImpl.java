package com.stockasticappbackend.service.sip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.dto.sip.SipRequest;
import com.stockasticappbackend.dto.sip.SipResponse;
import com.stockasticappbackend.dto.sip.SipTransactionResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.exception.InsufficientFundsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.SipMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.entity.Order;
import com.stockasticappbackend.model.entity.Sip;
import com.stockasticappbackend.model.entity.SipTransaction;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.NotificationType;
import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderType;
import com.stockasticappbackend.model.enums.SipFrequency;
import com.stockasticappbackend.model.enums.SipStatus;
import com.stockasticappbackend.model.enums.SipTransactionStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.SipRepository;
import com.stockasticappbackend.repository.SipTransactionRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WalletRepository;
import com.stockasticappbackend.service.notification.NotificationService;
import com.stockasticappbackend.service.order.OrderService;
import com.stockasticappbackend.service.stockprice.MarketHoursService;
import com.stockasticappbackend.service.stockprice.StockPriceService;
import static com.stockasticappbackend.util.Constants.KYC_NOT_APPROVED;
import static com.stockasticappbackend.util.Constants.STOCK_NOT_FOUND_ID;
import static com.stockasticappbackend.util.Constants.START_DATE_MUST_BE_FUTURE;
import static com.stockasticappbackend.util.Constants.START_DATE_MUST_BE_TRADING_DAY;
import static com.stockasticappbackend.util.Constants.USER_NOT_FOUND_EMAIL;
import static com.stockasticappbackend.util.Constants.WALLET_NOT_FOUND;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SipServiceImpl implements SipService {

    private static final String SIP_MSG_PREFIX = "Your SIP for ";

    private final SipRepository sipRepository;
    private final SipTransactionRepository sipTransactionRepository;
    private final StockRepository stockRepository;
    private final AppUserRepository userRepository;
    private final KycRepository kycRepository;
    private final WalletRepository walletRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final MarketHoursService marketHoursService;
    private final NotificationService notificationService;
    private final StockPriceService stockPriceService;
    private final SipMapper sipMapper;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @CacheEvict(value = "userSips", key = "#email")
    public SipResponse createSip(String email, SipRequest request) {
        AppUser user = findUser(email);

        validateKycApproval(user);
        validateNewSipStartDate(request.getStartDate());

        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new ResourceNotFoundException(STOCK_NOT_FOUND_ID + request.getStockId()));

        Sip sip = Sip.builder()
                .user(user)
                .stock(stock)
                .frequency(request.getFrequency())
                .quantity(request.getQuantity())
                .startDate(request.getStartDate())
                .nextExecutionDate(request.getStartDate())
                .status(SipStatus.ACTIVE)
                .build();

        sip = sipRepository.save(sip);
        
        notificationService.createNotification(user, "SIP Created", 
            SIP_MSG_PREFIX + stock.getSymbol() + " has been scheduled starting " + request.getStartDate(), 
            NotificationType.SIP);

        SipResponse response = sipMapper.toResponse(sip);
        enrichSipResponse(response);
        return response;
    }

    @Override
    @CacheEvict(value = "userSips", key = "#email")
    public SipResponse updateSip(String email, Long sipId, SipRequest request) {
        Sip sip = getSipEntity(email, sipId);
        validateUpdatedSipStartDate(sip, request.getStartDate());

        sip.setQuantity(request.getQuantity());
        
        if (!sip.getStartDate().isEqual(request.getStartDate())) {
            sip.setStartDate(request.getStartDate());
            if (sip.getNextExecutionDate().isBefore(request.getStartDate())) {
                 sip.setNextExecutionDate(request.getStartDate());
            } else {
                LocalDate next = calculateNextExecutionDate(sip.getStartDate(), sip.getFrequency(), LocalDate.now());
                sip.setNextExecutionDate(next);
            }
        }
        
        sip = sipRepository.save(sip);
        
        notificationService.createNotification(sip.getUser(), "SIP Updated", 
            SIP_MSG_PREFIX + sip.getStock().getSymbol() + " has been updated.", 
            NotificationType.SIP);
            
        SipResponse response = sipMapper.toResponse(sip);
        enrichSipResponse(response);
        return response;
    }

    @Override
    @CacheEvict(value = "userSips", key = "#email")
    public SipResponse toggleSipStatus(String email, Long sipId, SipStatus status) {
        Sip sip = getSipEntity(email, sipId);
        
        SipStatus oldStatus = sip.getStatus();
        sip.setStatus(status);
        
        if (oldStatus == SipStatus.PAUSED && status == SipStatus.ACTIVE) {
            if (sip.getNextExecutionDate().isBefore(LocalDate.now())) {
                LocalDate next = calculateNextExecutionDate(sip.getStartDate(), sip.getFrequency(), LocalDate.now());
                sip.setNextExecutionDate(next);
            }
        }

        sip = sipRepository.save(sip);
        
        notificationService.createNotification(sip.getUser(), "SIP " + status, 
            SIP_MSG_PREFIX + sip.getStock().getSymbol() + " is now " + status, 
            NotificationType.SIP);

        SipResponse response = sipMapper.toResponse(sip);
        enrichSipResponse(response);
        return response;
    }

    @Override
    public SipResponse getSip(String email, Long sipId) {
        SipResponse response = sipMapper.toResponse(getSipEntity(email, sipId));
        enrichSipResponse(response);
        return response;
    }

    @Override
    @Cacheable(value = "userSips", key = "#email")
    public List<SipResponse> getUserSips(String email) {
        AppUser user = findUser(email);
        List<SipResponse> responses = sipMapper.toResponseList(sipRepository.findByUser(user));
        responses.forEach(this::enrichSipResponse);
        return responses;
    }

    @Override
    public List<SipResponse> getSipsByStock(String email, Long stockId) {
        AppUser user = findUser(email);
        List<SipResponse> responses = sipMapper.toResponseList(sipRepository.findByUserAndStockId(user, stockId));
        responses.forEach(this::enrichSipResponse);
        return responses;
    }

    private void enrichSipResponse(SipResponse response) {
        if (response == null || response.getStock() == null) {
            return;
        }
        try {
            StockPriceResponse price = stockPriceService.getLatestPrice(response.getStock().getStockId());
            if (price != null) {
                response.getStock().setCurrentPrice(price.getPrice());
                response.getStock().setChangePercent(price.getChangePercent());
            }
        } catch (Exception e) {
            log.warn("Failed to enrich SIP stock price for {}: {}", response.getStock().getSymbol(), e.getMessage());
        }
    }

    @Override
    public Page<SipTransactionResponse> getSipHistory(String email, Pageable pageable) {
        AppUser user = findUser(email);
        Page<SipTransaction> transactions = sipTransactionRepository.findBySipUserOrderByExecutionDateDesc(user, pageable);
        return transactions.map(sipMapper::toTransactionResponse);
    }

    /**
     * Daily Scheduler at 9:15 AM
     */
    @Override
    @Scheduled(cron = "0 15 9 * * *", zone = "Asia/Kolkata")
    public void executeScheduledSips() {
        processDailySips(true);
    }

    /**
     * Runs on application startup to catch up on any missed SIPs
     * (e.g. if server was down/asleep at 9:15 AM)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Application Started: Checking for pending/missed SIPs...");
        try {
            processDailySips(false);
        } catch (Exception e) {
            log.warn("Skipping SIP startup execution: {}", e.getMessage());
        }
    }

    public void processDailySips(boolean notifyUpcoming) {
        if (!isSipTableAvailable()) {
            log.warn("Skipping SIP execution routine: required table missing (public.sips).");
            return;
        }

        log.info("Running SIP Execution Routine...");
        LocalDate today = LocalDate.now();

        if (notifyUpcoming) {
            checkAndNotifyUpcomingSips(today.plusDays(1));
        }

        if (!marketHoursService.isTradingDay(today)) {
            log.info("Today is market holiday. Postponing SIPs due today to next working day.");
            postponeSips(today);
            return;
        }

        List<Sip> dueSips = sipRepository.findByStatusAndNextExecutionDateLessThanEqual(SipStatus.ACTIVE, today);
        log.info("Found {} SIPs due for execution.", dueSips.size());

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Sip sip : dueSips) {
            try {
                template.execute(status -> {
                    processSipExecution(sip, today);
                    return null;
                });
            } catch (Exception e) {
                log.error("Failed to process SIP ID {}: {}", sip.getId(), e.getMessage());
            }
        }
    }

    private void checkAndNotifyUpcomingSips(LocalDate tomorrow) {
        List<Sip> upcomingSips = sipRepository.findByStatusAndNextExecutionDateLessThanEqual(SipStatus.ACTIVE, tomorrow);
        for (Sip sip : upcomingSips) {
            if (sip.getNextExecutionDate().isEqual(tomorrow)) {
                 notificationService.createNotification(sip.getUser(), "Upcoming SIP", 
                    SIP_MSG_PREFIX + sip.getStock().getSymbol() + " is scheduled for tomorrow.", 
                    NotificationType.SIP);
            }
        }
    }

    private void processSipExecution(Sip sipDetached, LocalDate executionDate) {
        Sip sip = sipRepository.findById(sipDetached.getId()).orElse(sipDetached);

        try {
            walletRepository.findByUser(sip.getUser())
                    .orElseThrow(() -> new ResourceNotFoundException(WALLET_NOT_FOUND));

            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setStockId(sip.getStock().getStockId());
            orderRequest.setOrderType(OrderType.BUY);
            orderRequest.setOrderMode(OrderMode.MARKET);
            orderRequest.setQuantity(sip.getQuantity());

            OrderResponse orderResp = orderService.placeOrder(sip.getUser().getEmail(), orderRequest);
            Order order = orderRepository.findById(orderResp.getOrderId()).orElse(null);

            logTransaction(sip, order, SipTransactionStatus.SUCCESS, null, 
                order != null ? order.getAverageFilledPrice() : BigDecimal.ZERO);
            
            notificationService.createNotification(sip.getUser(), "SIP Executed", 
                "SIP successful for " + sip.getQuantity() + " " + sip.getStock().getSymbol(), 
                NotificationType.SUCCESS);

            scheduleNextDate(sip);

        } catch (InsufficientFundsException e) {
            log.warn("SIP {} Skipped: Insufficient Funds", sip.getId());
            logTransaction(sip, null, SipTransactionStatus.SKIPPED_INSUFFICIENT_FUNDS, "Insufficient Funds", null);
            notificationService.createNotification(sip.getUser(), "SIP Skipped", 
                "SIP for " + sip.getStock().getSymbol() + " skipped due to insufficient funds.", 
                NotificationType.WARNING);
            
            scheduleNextDate(sip);
        } catch (Exception e) {
            log.error("SIP {} Failed: {}", sip.getId(), e.getMessage());
            logTransaction(sip, null, SipTransactionStatus.FAILED, e.getMessage(), null);
             scheduleNextDate(sip);
        }
    }

    private void scheduleNextDate(Sip sip) {
        LocalDate nextDate = calculateNextExecutionDate(sip.getStartDate(), sip.getFrequency(), LocalDate.now());
        sip.setNextExecutionDate(adjustToTradingDay(nextDate));
        sipRepository.save(sip);
    }
    
    private LocalDate calculateNextExecutionDate(LocalDate startDate, SipFrequency frequency, LocalDate afterDate) {
         long monthsDiff = ChronoUnit.MONTHS.between(startDate.withDayOfMonth(1), afterDate.withDayOfMonth(1));
         LocalDate next;

         if (frequency == SipFrequency.MONTHLY) {
             long baseN = Math.max(0, monthsDiff);
             next = startDate.plusMonths(baseN);
             while (!next.isAfter(afterDate)) {
                 baseN++;
                 next = startDate.plusMonths(baseN);
             }
         } else {
             long baseN = Math.max(0, monthsDiff / 12);
             next = startDate.plusYears(baseN);
             while (!next.isAfter(afterDate)) {
                 baseN++;
                 next = startDate.plusYears(baseN);
             }
         }
         
         return next;
    }

    private void postponeSips(LocalDate today) {
        List<Sip> dueSips = sipRepository.findByStatusAndNextExecutionDateLessThanEqual(SipStatus.ACTIVE, today);
        LocalDate nextTradingDay = adjustToTradingDay(today.plusDays(1));
        for (Sip sip : dueSips) {
            sip.setNextExecutionDate(nextTradingDay);
            sipRepository.save(sip);
        }
    }

    private boolean isSipTableAvailable() {
        try {
            Boolean tableAvailable = jdbcTemplate.queryForObject(
                    "SELECT to_regclass('public.sips') IS NOT NULL",
                    Boolean.class);
            return Boolean.TRUE.equals(tableAvailable);
        } catch (Exception e) {
            log.warn("Unable to verify SIP table availability: {}", e.getMessage());
            return false;
        }
    }

    private LocalDate adjustToTradingDay(LocalDate date) {
        LocalDate adjusted = date;
        while (!marketHoursService.isTradingDay(adjusted)) {
            adjusted = adjusted.plusDays(1);
        }
        return adjusted;
    }

    private void logTransaction(Sip sip, Order order, SipTransactionStatus status, String failureReason, BigDecimal price) {
        Sip sipRef = sip;
        if (sip.getId() != null) {
            try {
                sipRef = sipRepository.getReferenceById(sip.getId());
            } catch (Exception e) {
                log.debug("Could not fetch proxy for SIP {}: {}", sip.getId(), e.getMessage());
            }
        }
        
        SipTransaction tx = SipTransaction.builder()
                .sip(sipRef)
                .order(order)
                .executionDate(LocalDateTime.now())
                .status(status)
                .failureReason(failureReason)
                .price(price)
                .quantity(sip.getQuantity())
                .build();
        sipTransactionRepository.save(tx);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_EMAIL + email));
    }

    private void validateKycApproval(AppUser user) {
        Kyc kyc = kycRepository.findByUser(user).orElse(null);
        if (kyc == null || kyc.getKycStatus() != KycStatus.APPROVED) {
            throw new IllegalStateException(KYC_NOT_APPROVED);
        }
    }
    
    private Sip getSipEntity(String email, Long sipId) {
        AppUser user = findUser(email);
        Sip sip = sipRepository.findById(sipId)
                .orElseThrow(() -> new ResourceNotFoundException("SIP not found"));
        if (sip.getUser().getUserId() != user.getUserId()) {
             throw new ResourceNotFoundException("SIP not found for user");
        }
        return sip;
    }

    private void validateNewSipStartDate(LocalDate startDate) {
        LocalDate today = marketHoursService.getCurrentISTTime().toLocalDate();
        if (!startDate.isAfter(today)) {
            throw new IllegalArgumentException(START_DATE_MUST_BE_FUTURE);
        }
        if (!marketHoursService.isTradingDay(startDate)) {
            throw new IllegalArgumentException(START_DATE_MUST_BE_TRADING_DAY);
        }
    }

    private void validateUpdatedSipStartDate(Sip existingSip, LocalDate requestedStartDate) {
        if (requestedStartDate == null || requestedStartDate.isEqual(existingSip.getStartDate())) {
            return;
        }
        validateNewSipStartDate(requestedStartDate);
    }
}
