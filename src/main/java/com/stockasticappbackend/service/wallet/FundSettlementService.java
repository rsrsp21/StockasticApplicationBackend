package com.stockasticappbackend.service.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.stockasticappbackend.model.entity.Order;
import com.stockasticappbackend.model.entity.Wallet;
import com.stockasticappbackend.model.entity.WalletTransaction;
import com.stockasticappbackend.model.enums.TransactionStatus;
import com.stockasticappbackend.model.enums.TransactionType;
import com.stockasticappbackend.repository.OrderRepository;
import com.stockasticappbackend.repository.WalletRepository;
import com.stockasticappbackend.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling T+1 fund settlement.
 * Moves unsettled funds from lockedBalance to availableBalance after 24 hours.
 * Uses the Order entity's executedAt timestamp for settlement tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FundSettlementService {

    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PlatformTransactionManager transactionManager;

    /** Settlement period in hours (T+1 = 24 hours). */
    private static final int SETTLEMENT_HOURS = 24;
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    /**
     * Processes all sell orders that are eligible for settlement.
     * Orders older than 24 hours are settled by moving funds from lockedBalance to availableBalance.
     *
     * @return The number of orders settled
     */
    public int processSettlements() {
        LocalDateTime cutoffTime = LocalDateTime.now(IST_ZONE).minusHours(SETTLEMENT_HOURS);
        List<Order> settleableOrders = orderRepository.findSettleableSellOrders(cutoffTime);

        if (settleableOrders.isEmpty()) {
            log.info("No sell orders eligible for settlement at this time.");
            return 0;
        }

        log.info("Found {} sell orders eligible for settlement.", settleableOrders.size());

        int settledCount = 0;
        for (Order order : settleableOrders) {
            try {
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                Boolean settled = tx.execute(status -> {
                    try {
                        return settleOrder(order.getOrderId());
                    } catch (RuntimeException e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                });
                if (Boolean.TRUE.equals(settled)) {
                    settledCount++;
                }
            } catch (Exception e) {
                log.error("Failed to settle order ID {}: {}", order.getOrderId(), e.getMessage());
            }
        }

        log.info("Successfully settled {} sell orders.", settledCount);
        return settledCount;
    }

    /**
     * Settles a single sell order by moving funds from locked to available.
     * Handles edge case where locked funds may have already been used for buying.
     *
     * @param orderId The sell order id to settle
     */
    private boolean settleOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElse(null);
        if (order == null) {
            log.warn("Skipping settlement: order {} not found.", orderId);
            return false;
        }
        if (Boolean.TRUE.equals(order.getIsSettled())) {
            return false;
        }

        BigDecimal grossAmount = order.getAverageFilledPrice()
                .multiply(BigDecimal.valueOf(order.getFilledQuantity()));

        BigDecimal brokerage = order.getBrokerage() != null ? order.getBrokerage() : BigDecimal.ZERO;
        BigDecimal executedAmount = grossAmount.subtract(brokerage);

        Wallet wallet = walletRepository.findByUserIdForUpdate(order.getUser().getUserId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + order.getUser().getUserId()));

        BigDecimal currentLocked = wallet.getLockedBalance();

        BigDecimal amountToMove;
        if (currentLocked.compareTo(executedAmount) >= 0) {
            amountToMove = executedAmount;
        } else if (currentLocked.compareTo(BigDecimal.ZERO) > 0) {
            amountToMove = currentLocked;
            log.warn("Order {} - Locked balance ({}) < net executed amount ({}). Moving only available locked funds.",
                    order.getOrderId(), currentLocked, executedAmount);
        } else {
            amountToMove = BigDecimal.ZERO;
            log.warn("Order {} - No locked funds to settle (locked={}). Marking as settled without moving funds.",
                    order.getOrderId(), currentLocked);
        }

        if (amountToMove.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setLockedBalance(currentLocked.subtract(amountToMove));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amountToMove));
            walletRepository.save(wallet);

            createSettlementTransactionIfMissing(wallet, amountToMove, order.getOrderId());
        }

        order.setIsSettled(true);
        order.setSettledAt(LocalDateTime.now(IST_ZONE));
        orderRepository.save(order);

        log.debug("Settled order ID {} - {} INR moved to available for user {}",
                order.getOrderId(), amountToMove, order.getUser().getUserId());
        return true;
    }

    /**
     * Creates a wallet transaction record for the settlement.
     */
    private void createSettlementTransactionIfMissing(Wallet wallet, BigDecimal amount, Long orderId) {
        String referenceId = "SETTLE-" + orderId;
        if (transactionRepository.findByReferenceId(referenceId).isPresent()) {
            return;
        }

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.CREDIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setReferenceId(referenceId);
        transaction.setDescription("Funds settled from sell order");
        try {
            transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ignored) {
            // Another concurrent worker inserted the same reference first.
        }
    }
}
