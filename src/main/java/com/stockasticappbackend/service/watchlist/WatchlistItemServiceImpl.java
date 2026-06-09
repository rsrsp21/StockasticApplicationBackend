package com.stockasticappbackend.service.watchlist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.watchlist.AddStockToWatchlistRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistItemResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.exception.DuplicateResourceException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.WatchlistMapper;
import com.stockasticappbackend.model.entity.Stock;
import com.stockasticappbackend.model.entity.StockPrice;
import com.stockasticappbackend.model.entity.Watchlist;
import com.stockasticappbackend.model.entity.WatchlistItem;
import com.stockasticappbackend.repository.StockPriceRepository;
import com.stockasticappbackend.repository.StockRepository;
import com.stockasticappbackend.repository.WatchlistItemRepository;
import com.stockasticappbackend.repository.WatchlistRepository;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WatchlistItemServiceImpl implements WatchlistItemService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final WatchlistMapper watchlistMapper;

    /*
     * Watchlist Items
     */

    @Override
    public WatchlistItemResponse addStockToWatchlist(
            Long watchlistId, Long userId, AddStockToWatchlistRequest request) {

        Watchlist watchlist = findWatchlistByIdAndUserId(watchlistId, userId);

        Stock stock = stockRepository.findById(request.getStockId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        Constants.STOCK_NOT_FOUND_ID + request.getStockId()));

        if (watchlistItemRepository.existsByWatchlistAndStock(watchlist, stock)) {
            throw new DuplicateResourceException(Constants.STOCK_ALREADY_IN_WATCHLIST);
        }

        WatchlistItem item = new WatchlistItem();
        item.setWatchlist(watchlist);
        item.setStock(stock);

        return watchlistMapper.toItemResponse(
                watchlistItemRepository.save(item));
    }

    @Override
    public void removeStockFromWatchlist(Long watchlistId, Long userId, Long stockId) {
        // Validation: ensures user owns watchlist
        findWatchlistByIdAndUserId(watchlistId, userId);

        WatchlistItem item = watchlistItemRepository
                .findByWatchlistIdAndStockId(watchlistId, stockId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.STOCK_NOT_IN_WATCHLIST));

        watchlistItemRepository.delete(item);
    }

    /*
     * ========================================================================
     * Watchlist Items (WITH PRICES)
     * ========================================================================
     */

    @Override
    public List<WatchlistItemResponse> getWatchlistItemsWithPrices(
            Long watchlistId, Long userId) {

        Watchlist watchlist = findWatchlistByIdAndUserId(watchlistId, userId);

        return watchlistItemRepository
                .findByWatchlistOrderByAddedAtDesc(watchlist)
                .stream()
                .map(this::mapAndEnrichWithPrice)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<WatchlistItemResponse> getWatchlistItemsWithPricesPaged(
            Long watchlistId, Long userId,
            int page, int size, String sortBy, String sortDir) {

        Watchlist watchlist = findWatchlistByIdAndUserId(watchlistId, userId);

        // Use simple pagination without Sort - the repository query already has ORDER BY added_at DESC
        // Adding Sort here would cause Spring Data JPA to append additional ORDER BY clauses
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchlistItem> pageResult = watchlistItemRepository.findByWatchlistOrderByAddedAtDesc(
                watchlist, pageable);

        List<WatchlistItemResponse> content = pageResult.getContent()
                .stream()
                .map(this::mapAndEnrichWithPrice)
                .collect(Collectors.toList());

        return PageResponse.<WatchlistItemResponse>builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build();
    }

    /*
     * Utility
     */

    @Override
    public boolean isStockInUserWatchlists(Long stockId, Long userId) {
        return !watchlistItemRepository
                .findByStockIdAndUserId(stockId, userId)
                .isEmpty();
    }

    @Override
    public List<WatchlistResponse> getWatchlistsContainingStock(Long stockId, Long userId) {
        return watchlistItemRepository
                .findByStockIdAndUserId(stockId, userId)
                .stream()
                .map(item -> watchlistMapper.toResponse(item.getWatchlist()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getWatchlistIdsContainingStock(Long stockId, Long userId) {
        return watchlistItemRepository
                .findByStockIdAndUserId(stockId, userId)
                .stream()
                .map(item -> item.getWatchlist().getWatchlistId())
                .distinct()
                .collect(Collectors.toList());
    }

    /*
     * Helpers
     */

    private Watchlist findWatchlistByIdAndUserId(Long watchlistId, Long userId) {
        return watchlistRepository
                .findByWatchlistIdAndUserId(watchlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.WATCHLIST_NOT_FOUND));
    }

    /**
     * Map WatchlistItem to response and enrich embedded stock with latest price
     * data.
     */
    private WatchlistItemResponse mapAndEnrichWithPrice(WatchlistItem item) {
        WatchlistItemResponse response = watchlistMapper.toItemResponse(item);

        // Enrich the embedded stock response with price data
        if (response.getStock() != null) {
            List<StockPrice> latestPrices = stockPriceRepository.findByStockOrderByPriceTimeDesc(
                    item.getStock(), PageRequest.of(0, 2)).getContent();

            if (!latestPrices.isEmpty()) {
                StockPrice latestPrice = latestPrices.get(0);
                response.getStock().setCurrentPrice(latestPrice.getIntervalClose());

                // Volume logic: Use "last but one" if latest is 0
                Long volume = latestPrice.getIntervalVolume();
                if ((volume == null || volume == 0L) && latestPrices.size() > 1) {
                    StockPrice previousPrice = latestPrices.get(1);
                    if (previousPrice.getIntervalVolume() != null && previousPrice.getIntervalVolume() > 0L) {
                        volume = previousPrice.getIntervalVolume();
                    }
                }
                response.getStock().setVolume(volume);
                response.getStock().setChangePercent(calculateChangePercent(latestPrice));
            }
        }

        return response;
    }

    /**
     * Calculate change percentage from baseline (previousClose or openPrice).
     */
    private BigDecimal calculateChangePercent(StockPrice stockPrice) {
        BigDecimal baseline = stockPrice.getPreviousClose();
        if (baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            baseline = stockPrice.getIntervalOpen();
        }

        if (baseline != null && baseline.compareTo(BigDecimal.ZERO) != 0 && stockPrice.getIntervalClose() != null) {
            BigDecimal change = stockPrice.getIntervalClose().subtract(baseline);
            return change.divide(baseline, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }
        return null;
    }
}
