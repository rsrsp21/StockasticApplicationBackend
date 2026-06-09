package com.stockasticappbackend.service.holdings;

import static com.stockasticappbackend.util.Constants.INSUFFICIENT_HOLDINGS;
import static com.stockasticappbackend.util.Constants.USER_NOT_FOUND_EMAIL;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.stockasticappbackend.dto.order.HoldingsResponse;
import com.stockasticappbackend.dto.stockprice.StockPriceResponse;
import com.stockasticappbackend.event.HoldingsReducedEvent;
import com.stockasticappbackend.exception.InsufficientHoldingsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.HoldingsMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Holdings;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.HoldingsRepository;
import com.stockasticappbackend.service.stockprice.StockPriceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HoldingsServiceImpl implements HoldingsService {

    private final HoldingsRepository holdingsRepository;
    private final AppUserRepository userRepository;
    private final StockPriceService stockPriceService;
    private final HoldingsMapper holdingsMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Retrieves the holdings portfolio for a user by email
    @Override
    @Transactional(readOnly = true)
    public List<HoldingsResponse> getHoldings(String email) {
        AppUser user = findUser(email);
        List<Holdings> holdings = holdingsRepository.findByUser(user);
        return holdings.stream().map(this::mapToHoldingsResponse).collect(Collectors.toList());
    }

    // Retrieves a single holding for a user and specific stock
    @Override
    @Transactional(readOnly = true)
    public HoldingsResponse getHoldingByStock(String email, Long stockId) {
        AppUser user = findUser(email);
        Holdings holdings = holdingsRepository.findByUserIdAndStockId(user.getUserId(), stockId)
            .orElse(null);
        
        if (holdings == null) {
            return null;
        }
        
        return mapToHoldingsResponse(holdings);
    }

    // Validates if the user has sufficient available holdings to sell
    @Override
    public void validateHoldingsForSell(AppUser user, Stock stock, int quantity) {
        Holdings holdings = getHoldingsOrThrow(user, stock);

        if (holdings.getAvailableQuantity() < quantity) {
            throw new InsufficientHoldingsException(INSUFFICIENT_HOLDINGS);
        }
    }

    // Blocks shares in the user's holdings (moves from available to locked)
    @Override
    public void blockShares(AppUser user, Stock stock, int quantity) {
        Holdings holdings = getHoldingsOrThrow(user, stock);
        holdings.setLockedQuantity(holdings.getLockedQuantity() + quantity);
        holdingsRepository.save(holdings);
    }

    // Releases blocked shares back to available quantity
    @Override
    public void releaseBlockedShares(AppUser user, Stock stock, int quantity) {
        Holdings holdings = holdingsRepository.findByUserAndStockForUpdate(user, stock).orElse(null);
        if (holdings != null) {
            holdings.setLockedQuantity(holdings.getLockedQuantity() - quantity);
            holdingsRepository.save(holdings);
        }
    }

    // Credits holdings to the user's account after a successful buy order
    @Override
    public void creditHoldings(AppUser user, Stock stock, int quantity, BigDecimal executedAmount) {
        Holdings holdings = holdingsRepository.findByUserAndStockForUpdate(user, stock)
            .orElse(Holdings.builder()
                .user(user)
                .stock(stock)
                .quantity(0)
                .lockedQuantity(0)
                .averagePrice(BigDecimal.ZERO)
                .build());

        int oldQty = holdings.getQuantity();
        BigDecimal oldAvg = holdings.getAveragePrice();
        int newQty = oldQty + quantity;
        
        BigDecimal oldValue = oldAvg.multiply(BigDecimal.valueOf(oldQty));
        BigDecimal newValue = oldValue.add(executedAmount);
        BigDecimal newAvg = newQty > 0 
            ? newValue.divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        holdings.setQuantity(newQty);
        holdings.setAveragePrice(newAvg);
        holdingsRepository.save(holdings);
    }

    // Debits holdings from the user's account after a successful sell order
    @Override
    public BigDecimal debitHoldings(AppUser user, Stock stock, int quantity, BigDecimal executedAmount, BigDecimal averagePrice) {
        Holdings holdings = getHoldingsOrThrow(user, stock);

        BigDecimal costBasis = holdings.getAveragePrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal realizedPnl = executedAmount.subtract(costBasis);
        
        if (holdings.getTotalRealizedPnl() == null) {
            holdings.setTotalRealizedPnl(BigDecimal.ZERO);
        }
        holdings.setTotalRealizedPnl(holdings.getTotalRealizedPnl().add(realizedPnl));

        holdings.setLockedQuantity(holdings.getLockedQuantity() - quantity);
        holdings.setQuantity(holdings.getQuantity() - quantity);
        int remainingQuantity = Math.max(holdings.getQuantity(), 0);
        
        if (holdings.getQuantity() <= 0) {
            holdingsRepository.delete(holdings);
        } else {
            holdingsRepository.save(holdings);
        }

        eventPublisher.publishEvent(
            new HoldingsReducedEvent(this, user.getUserId(), stock.getStockId(), remainingQuantity)
        );

        return realizedPnl;
    }

    // Gets the average buy price for a specific stock holding
    @Override
    public BigDecimal getAveragePrice(AppUser user, Stock stock) {
         Holdings holdings = getHoldingsOrThrow(user, stock);
         return holdings.getAveragePrice();
    }

    // Helper method to find user by email
    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_EMAIL + email));
    }

    // Helper method to get holdings or throw exception - UPDATED TO USE LOCKING
    private Holdings getHoldingsOrThrow(AppUser user, Stock stock) {
        return holdingsRepository.findByUserAndStockForUpdate(user, stock)
            .orElseThrow(() -> new InsufficientHoldingsException(INSUFFICIENT_HOLDINGS));
    }

    // Maps Holdings entity to HoldingsResponse DTO
    private HoldingsResponse mapToHoldingsResponse(Holdings holdings) {
        HoldingsResponse response = holdingsMapper.toResponse(holdings);
        
        if (holdings.getAveragePrice() != null && holdings.getQuantity() != null) {
            response.setInvestedAmount(holdings.getAveragePrice()
                .multiply(BigDecimal.valueOf(holdings.getQuantity())));
        } else {
            response.setInvestedAmount(BigDecimal.ZERO);
        }

        try {
            StockPriceResponse priceResponse = stockPriceService.getLatestPriceBySymbol(holdings.getStock().getSymbol());
            enrichWithMarketData(response, priceResponse);
        } catch (Exception e) {
            log.error("Failed to fetch price for holding {}: {}", holdings.getStock().getSymbol(), e.getMessage());
            StockPriceResponse dummy = StockPriceResponse.builder()
                .price(holdings.getAveragePrice())
                .openPrice(holdings.getAveragePrice())
                .previousClose(holdings.getAveragePrice())
                .changePercent(BigDecimal.ZERO)
                .build();
            enrichWithMarketData(response, dummy);
        }
        
        return response;
    }

    // Enriches response with current market data and P&L
    private void enrichWithMarketData(HoldingsResponse response, StockPriceResponse stockPrice) {
        if (response == null || stockPrice == null || stockPrice.getPrice() == null) {
            return;
        }

        BigDecimal currentPrice = stockPrice.getPrice();
        response.setCurrentPrice(currentPrice);
        
        BigDecimal currentValue = currentPrice.multiply(BigDecimal.valueOf(response.getQuantity()));
        response.setCurrentValue(currentValue);
        
        BigDecimal profitLoss = currentValue.subtract(response.getInvestedAmount());
        response.setProfitLoss(profitLoss);
        
        if (response.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profitLossPercent = profitLoss
                .divide(response.getInvestedAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            response.setProfitLossPercent(profitLossPercent);
        } else {
            response.setProfitLossPercent(BigDecimal.ZERO);
        }

        BigDecimal baselinePrice = stockPrice.getPreviousClose();
        if (baselinePrice == null || baselinePrice.compareTo(BigDecimal.ZERO) == 0) {
            baselinePrice = stockPrice.getOpenPrice();
        }

        if (baselinePrice != null && baselinePrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dayChange = currentPrice.subtract(baselinePrice);
            response.setDayChange(dayChange);
            
            BigDecimal dayChangePercent = dayChange
                .divide(baselinePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            response.setDayChangePercent(dayChangePercent);
        } else if (stockPrice.getChangePercent() != null) {
             response.setDayChangePercent(stockPrice.getChangePercent());
             BigDecimal open = currentPrice.divide(
                 BigDecimal.ONE.add(stockPrice.getChangePercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)), 
                 4, RoundingMode.HALF_UP
             );
             response.setDayChange(currentPrice.subtract(open));
        }

        BigDecimal realized = response.getRealizedPnl() != null ? response.getRealizedPnl() : BigDecimal.ZERO;
        response.setTotalPnl(response.getProfitLoss().add(realized));
    }
}
