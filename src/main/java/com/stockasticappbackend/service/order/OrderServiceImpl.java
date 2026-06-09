package com.stockasticappbackend.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.order.OrderRequest;
import com.stockasticappbackend.dto.order.OrderResponse;
import com.stockasticappbackend.dto.stock.StockResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.exception.InsufficientFundsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.OrderMapper;
import com.stockasticappbackend.mapper.StockMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.entity.Order;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.entity.WalletTransaction;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.MarketSession;
import com.stockasticappbackend.model.enums.OrderMode;
import com.stockasticappbackend.model.enums.OrderStatus;
import com.stockasticappbackend.model.enums.OrderType;
import com.stockasticappbackend.model.enums.TransactionStatus;
import com.stockasticappbackend.model.enums.TransactionType;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WalletRepository;
import com.stockasticappbackend.repository.WalletTransactionRepository;
import com.stockasticappbackend.service.holdings.HoldingsService;
import com.stockasticappbackend.service.stockprice.MarketHoursService;
import com.stockasticappbackend.service.stockprice.StockPriceService;
import static com.stockasticappbackend.util.Constants.INSUFFICIENT_FUNDS;
import static com.stockasticappbackend.util.Constants.KYC_NOT_APPROVED;
import static com.stockasticappbackend.util.Constants.LIMIT_PRICE_REQUIRED;
import static com.stockasticappbackend.util.Constants.ORDER_NOT_FOUND;
import static com.stockasticappbackend.util.Constants.STOCK_NOT_FOUND_ID;
import static com.stockasticappbackend.util.Constants.USER_NOT_FOUND_EMAIL;
import static com.stockasticappbackend.util.Constants.WALLET_NOT_FOUND;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final StockRepository stockRepository;
    private final AppUserRepository userRepository;
    private final KycRepository kycRepository;
    private final StockPriceService stockPriceService;
    private final MarketHoursService marketHoursService;
    private final HoldingsService holdingsService;
    private final OrderMapper orderMapper;
    private final StockMapper stockMapper;
    @Override
    public OrderResponse placeOrder(String email, OrderRequest request) {
        AppUser user = findUser(email);
        validateKycApproval(user);
        
        Stock stock = findStock(request.getStockId());
        MarketSession session = getCurrentSession();
        boolean isAmo = !marketHoursService.isMarketOpen();

        if (isAmo && request.getOrderMode() == OrderMode.MARKET) {
            log.info("Market AMO order placed by {} for {}. Will execute at opening price.",
                email, stock.getSymbol());
        }

        if (request.getOrderMode() == OrderMode.LIMIT && request.getPrice() == null) {
            throw new IllegalArgumentException(LIMIT_PRICE_REQUIRED);
        }

        BigDecimal backendPrice = getBackendPrice(stock);
        BigDecimal orderPrice = request.getOrderMode() == OrderMode.LIMIT 
            ? request.getPrice() 
            : backendPrice;

        BigDecimal totalAmount = orderPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        Order order;
        if (request.getOrderType() == OrderType.BUY) {
            order = processBuyOrder(user, stock, request, session, isAmo, orderPrice, totalAmount, backendPrice);
        } else {
            order = processSellOrder(user, stock, request, session, isAmo, orderPrice, totalAmount, backendPrice);
        }

        log.info("Order {} created: {} {} shares of {} at {} (session: {}, AMO: {})", 
            order.getOrderId(), request.getOrderType(), request.getQuantity(), 
            stock.getSymbol(), orderPrice, session, isAmo);

        return orderMapper.toResponse(order);
    }
    private boolean shouldExecuteImmediately(OrderMode mode, OrderType type, BigDecimal limitPrice, BigDecimal marketPrice) {
        if (mode == OrderMode.MARKET) return true;
        if (mode == OrderMode.LIMIT) {
             if (type == OrderType.BUY) return marketPrice.compareTo(limitPrice) <= 0;
             if (type == OrderType.SELL) return marketPrice.compareTo(limitPrice) >= 0;
        }
        return false;
    }
    private Order buildOrder(AppUser user, Stock stock, OrderRequest request, MarketSession session, 
            boolean isAmo, BigDecimal price, BigDecimal totalAmount, OrderStatus status, boolean shouldExecute, 
            BigDecimal currentMarketPrice, BigDecimal blockedAmount, BigDecimal fundedFromLocked) {
        
        return Order.builder()
            .user(user)
            .stock(stock)
            .orderType(request.getOrderType())
            .orderMode(request.getOrderMode())
            .status(status)
            .marketSession(session)
            .isAmo(isAmo)
            .quantity(request.getQuantity())
            .filledQuantity(shouldExecute ? request.getQuantity() : 0)
            .price(price)
            .averageFilledPrice(shouldExecute ? currentMarketPrice : null)
            .totalAmount(totalAmount)
            .blockedAmount(blockedAmount)
            .blockedQuantity(request.getOrderType() == OrderType.SELL ? request.getQuantity() : 0)
            .fundedFromLocked(fundedFromLocked)
            .executedAt(shouldExecute ? LocalDateTime.now() : null)
            .build();
    }

    private Order processBuyOrder(AppUser user, Stock stock, OrderRequest request,
            MarketSession session, boolean isAmo, BigDecimal price, 
            BigDecimal totalAmount, BigDecimal currentMarketPrice) {
        
        Wallet wallet = getWallet(user);
        List<Order> pendingBuyOrders = orderRepository.findByUserAndStatus(user, OrderStatus.PENDING)
            .stream().filter(o -> o.getOrderType() == OrderType.BUY)
            .collect(Collectors.toList());
            
        BigDecimal pendingBlocked = pendingBuyOrders.stream()
            .map(Order::getBlockedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal realUnsettled = wallet.getLockedBalance().subtract(pendingBlocked);
        if (realUnsettled.compareTo(BigDecimal.ZERO) < 0) {
            realUnsettled = BigDecimal.ZERO;
        }
        
        BigDecimal buyingPower = wallet.getAvailableBalance().add(realUnsettled);

        if (buyingPower.compareTo(totalAmount) < 0) {
            throw new InsufficientFundsException(INSUFFICIENT_FUNDS);
        }

        BigDecimal available = wallet.getAvailableBalance();
        BigDecimal fundedFromAvailable = totalAmount.min(available);
        BigDecimal fundedFromLocked = totalAmount.subtract(fundedFromAvailable);

        wallet.setAvailableBalance(available.subtract(fundedFromAvailable));
        wallet.setLockedBalance(wallet.getLockedBalance().add(fundedFromAvailable));
        walletRepository.save(wallet);

        boolean shouldExecute = !isAmo && shouldExecuteImmediately(request.getOrderMode(), OrderType.BUY, price, currentMarketPrice);
        OrderStatus status = shouldExecute ? OrderStatus.FILLED : OrderStatus.PENDING;

        Order order = buildOrder(user, stock, request, session, isAmo, price, totalAmount, status, shouldExecute, currentMarketPrice, totalAmount, fundedFromLocked);
        order = orderRepository.save(order);

        if (shouldExecute) {
            executeBuyOrder(user, stock, order, wallet);
        }

        return order;
    }
    private void executeBuyOrder(AppUser user, Stock stock, Order order, Wallet wallet) {
        BigDecimal executedAmount = order.getAverageFilledPrice()
            .multiply(BigDecimal.valueOf(order.getFilledQuantity()));
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(order.getBlockedAmount()));
        if (executedAmount.compareTo(order.getBlockedAmount()) < 0) {
            BigDecimal refund = order.getBlockedAmount().subtract(executedAmount);
            BigDecimal fundedFromLocked = order.getFundedFromLocked() != null ? order.getFundedFromLocked() : BigDecimal.ZERO;
            BigDecimal refundToLocked = refund.min(fundedFromLocked);
            BigDecimal refundToAvailable = refund.subtract(refundToLocked);
            
            wallet.setLockedBalance(wallet.getLockedBalance().add(refundToLocked));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(refundToAvailable));
        }
        
        walletRepository.save(wallet);
        holdingsService.creditHoldings(user, stock, order.getFilledQuantity(), executedAmount);
        createTransaction(wallet, executedAmount, TransactionType.DEBIT, 
            TransactionStatus.SUCCESS, "ORD-" + order.getOrderId(),
            "Buy " + order.getFilledQuantity() + " " + stock.getSymbol());
    }
    private Order processSellOrder(AppUser user, Stock stock, OrderRequest request,
            MarketSession session, boolean isAmo, BigDecimal price, 
            BigDecimal totalAmount, BigDecimal currentMarketPrice) {
        holdingsService.validateHoldingsForSell(user, stock, request.getQuantity());
        holdingsService.blockShares(user, stock, request.getQuantity());

        boolean shouldExecute = !isAmo && shouldExecuteImmediately(request.getOrderMode(), OrderType.SELL, price, currentMarketPrice);
        OrderStatus status = shouldExecute ? OrderStatus.FILLED : OrderStatus.PENDING;

        Order order = buildOrder(user, stock, request, session, isAmo, price, totalAmount, status, shouldExecute, currentMarketPrice, null, null);
        order = orderRepository.save(order);

        if (shouldExecute) {
            BigDecimal avgPrice = holdingsService.getAveragePrice(user, stock);
            executeSellOrder(user, stock, order, avgPrice);
        }

        return order;
    }
    private void executeSellOrder(AppUser user, Stock stock, Order order, BigDecimal averageBuyPrice) {
        BigDecimal grossAmount = order.getAverageFilledPrice()
            .multiply(BigDecimal.valueOf(order.getFilledQuantity()));
        BigDecimal percentageFee = grossAmount.multiply(new BigDecimal("0.001"));
        BigDecimal cappedFee = percentageFee.min(new BigDecimal("20.00"));
        BigDecimal brokerageFee = cappedFee.max(new BigDecimal("5.00"));

        BigDecimal netAmount = grossAmount.subtract(brokerageFee);
        BigDecimal realizedPnl = holdingsService.debitHoldings(user, stock, order.getFilledQuantity(), grossAmount, averageBuyPrice);
        
        order.setRealizedPnl(realizedPnl);
        order.setBrokerage(brokerageFee);
        orderRepository.save(order);
        if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
            Wallet wallet = getWallet(user);
            wallet.setLockedBalance(wallet.getLockedBalance().add(netAmount));
            walletRepository.save(wallet);

            createTransaction(wallet, netAmount, TransactionType.CREDIT,
                TransactionStatus.SUCCESS, "ORD-" + order.getOrderId(),
                String.format("Sell %d %s (Unsettled) | Fee: ₹%s", 
                    order.getFilledQuantity(), stock.getSymbol(), brokerageFee));
        } else {
             log.warn("Order {} executed but brokerage (₹{}) exceeds trade value (₹{}). No funds credited.", 
                 order.getOrderId(), brokerageFee, grossAmount);
        }
    }
    @Override
    public OrderResponse cancelOrder(String email, Long orderId) {
        AppUser user = findUser(email);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));

        if (order.getUser().getUserId() != user.getUserId()) {
            throw new ResourceNotFoundException(ORDER_NOT_FOUND);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (order.getOrderType() == OrderType.BUY) {
            Wallet wallet = getWallet(user);
            
            BigDecimal refund = order.getBlockedAmount();
            BigDecimal fundedFromLocked = order.getFundedFromLocked() != null ? order.getFundedFromLocked() : BigDecimal.ZERO;
            
            BigDecimal refundToLocked = refund.min(fundedFromLocked);
            BigDecimal refundToAvailable = refund.subtract(refundToLocked);

            wallet.setLockedBalance(wallet.getLockedBalance().subtract(refundToAvailable));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(refundToAvailable));
            
            walletRepository.save(wallet);

        } else {
            holdingsService.releaseBlockedShares(user, order.getStock(), order.getQuantity());
        }

        log.info("Order {} cancelled by user {}", orderId, email);
        return orderMapper.toResponse(order);
    }
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(String email) {
        AppUser user = findUser(email);
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orderMapper.toResponseList(orders);
    }
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String email, Long orderId) {
        AppUser user = findUser(email);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));

        if (order.getUser().getUserId() != user.getUserId()) {
            throw new ResourceNotFoundException(ORDER_NOT_FOUND);
        }

        return orderMapper.toResponse(order);
    }



    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "mostTraded", key = "#limit")
    public List<StockResponse> getMostTradedStocks(int limit) {
        List<Stock> stocks = orderRepository.findMostTradedStocks(PageRequest.of(0, limit));
        return stocks.stream().map(stock -> {
            StockResponse response = stockMapper.toResponse(stock);
            try {
                StockPriceResponse priceData = stockPriceService.getLatestPriceBySymbol(stock.getSymbol());
                if (priceData != null) {
                    response.setCurrentPrice(priceData.getPrice());
                    response.setChangePercent(priceData.getChangePercent());
                    response.setVolume(priceData.getVolume());
                }
            } catch (Exception e) {
                log.debug("Could not fetch price for stock {}: {}", stock.getSymbol(), e.getMessage());
            }
            return response;
        }).toList();
    }
    @Override
    @Transactional
    public void processDailyMarketOpenOrders() {
        log.info("Starting Daily Market Open Order Processing (AMO Execution)...");

        if (!marketHoursService.isMarketOpen()) {
            log.warn("AMO Processing triggered but Market is CLOSED. Skipping execution.");
            return;
        }

        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);
        log.info("Found {} pending orders to process.", pendingOrders.size());

        for (Order order : pendingOrders) {
            try {
                Stock stock = order.getStock();
                StockPriceResponse priceResp = stockPriceService.getLatestPriceBySymbol(stock.getSymbol());
                BigDecimal currentPrice = priceResp.getPrice();

                boolean shouldExecute = shouldExecuteImmediately(order.getOrderMode(), order.getOrderType(), order.getPrice(), currentPrice);

                if (shouldExecute) {
                    log.info("Executing AMO Order ID: {} for {} (Type: {}, Mode: {})", 
                        order.getOrderId(), stock.getSymbol(), order.getOrderType(), order.getOrderMode());

                    order.setFilledQuantity(order.getQuantity());
                    order.setAverageFilledPrice(currentPrice); 
                    order.setExecutedAt(LocalDateTime.now());
                    order.setStatus(OrderStatus.FILLED);
                    
                    order = orderRepository.save(order);

                    if (order.getOrderType() == OrderType.BUY) {
                        Wallet wallet = getWallet(order.getUser());
                        executeBuyOrder(order.getUser(), stock, order, wallet);
                    } else {
                        BigDecimal avgPrice = holdingsService.getAveragePrice(order.getUser(), stock);
                        executeSellOrder(order.getUser(), stock, order, avgPrice);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to execute AMO Order {}: {}", order.getOrderId(), e.getMessage());
            }
        }
        log.info("Completed AMO Processing.");
    }
    private MarketSession getCurrentSession() {
        if (!marketHoursService.isTodayTradingDay()) {
            return MarketSession.AFTER_MARKET; 
        }
        if (marketHoursService.isBeforeMarketOpen()) {
            return MarketSession.PRE_MARKET;
        }
        if (marketHoursService.isAfterMarketClose()) {
            return MarketSession.AFTER_MARKET;
        }
        return MarketSession.MARKET_HOURS;
    }
    private BigDecimal getBackendPrice(Stock stock) {
        try {
            return stockPriceService.getLatestPriceBySymbol(stock.getSymbol()).getPrice();
        } catch (Exception e) {
            log.error("Could not fetch price for {}: {}", stock.getSymbol(), e.getMessage());
            throw new IllegalStateException("Cannot determine price for " + stock.getSymbol());
        }
    }
    private Wallet getWallet(AppUser user) {
        return walletRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException(WALLET_NOT_FOUND));
    }
    private void createTransaction(Wallet wallet, BigDecimal amount, TransactionType type,
            TransactionStatus status, String referenceId, String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setReferenceId(referenceId);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }
    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_EMAIL + email));
    }
    private Stock findStock(Long stockId) {
        return stockRepository.findById(stockId)
            .orElseThrow(() -> new ResourceNotFoundException(STOCK_NOT_FOUND_ID + stockId));
    }
    private void validateKycApproval(AppUser user) {
        Kyc kyc = kycRepository.findByUser(user).orElse(null);
        if (kyc == null || kyc.getKycStatus() != KycStatus.APPROVED) {
            throw new IllegalStateException(KYC_NOT_APPROVED);
        }
    }

}

