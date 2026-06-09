package com.stockasticappbackend.service.watchlist;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistCreateRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistDetailResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistUpdateRequest;
import com.stockasticappbackend.exception.DuplicateResourceException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.WatchlistMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Watchlist;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.WatchlistRepository;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WatchlistServiceImpl implements WatchlistService {

        private final WatchlistRepository watchlistRepository;
        private final AppUserRepository userRepository;
        private final WatchlistMapper watchlistMapper;

        /*
         * Watchlists
         */

        @Override
        public List<WatchlistResponse> getUserWatchlists(Long userId) {
                AppUser user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND_ID + userId));

                return watchlistMapper.toResponseList(
                                watchlistRepository.findByUserOrderByCreatedAtDesc(user));
        }

        @Override
        public PageResponse<WatchlistResponse> getUserWatchlistsPaged(
                        Long userId, int page, int size, String sortBy, String sortDir) {

                AppUser user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND_ID + userId));

                Pageable pageable = PageRequest.of(page, size);
                Page<Watchlist> pageResult = watchlistRepository.findByUserOrderByCreatedAtDesc(user, pageable);

                return toPageResponse(pageResult);
        }

        @Override
        public WatchlistDetailResponse getWatchlistById(Long watchlistId, Long userId) {
                return watchlistMapper.toDetailResponse(
                                findWatchlistByIdAndUserId(watchlistId, userId));
        }

        @Override
        public WatchlistResponse createWatchlist(Long userId, WatchlistCreateRequest request) {
                AppUser user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND_ID + userId));

                if (watchlistRepository.existsByNameAndUser(request.getName(), user)) {
                        throw new DuplicateResourceException(
                                        String.format(Constants.WATCHLIST_DUPLICATE_NAME, request.getName()));
                }

                Watchlist watchlist = new Watchlist();
                watchlist.setUser(user);
                watchlist.setName(request.getName());

                return watchlistMapper.toResponse(
                                watchlistRepository.save(watchlist));
        }

        @Override
        public WatchlistResponse updateWatchlist(
                        Long watchlistId, Long userId, WatchlistUpdateRequest request) {

                Watchlist watchlist = findWatchlistByIdAndUserId(watchlistId, userId);

                if (!watchlist.getName().equals(request.getName())
                                && watchlistRepository.existsByNameAndUser(
                                                request.getName(), watchlist.getUser())) {

                        throw new DuplicateResourceException(
                                        String.format(Constants.WATCHLIST_DUPLICATE_NAME, request.getName()));
                }

                watchlist.setName(request.getName());
                return watchlistMapper.toResponse(
                                watchlistRepository.save(watchlist));
        }

        @Override
        public void deleteWatchlist(Long watchlistId, Long userId) {
                watchlistRepository.delete(
                                findWatchlistByIdAndUserId(watchlistId, userId));
        }

        /*
         * Helpers
         */

        private Watchlist findWatchlistByIdAndUserId(Long watchlistId, Long userId) {
                return watchlistRepository
                                .findByWatchlistIdAndUserId(watchlistId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException(Constants.WATCHLIST_NOT_FOUND));
        }

        private PageResponse<WatchlistResponse> toPageResponse(Page<Watchlist> page) {
                return PageResponse.<WatchlistResponse>builder()
                                .content(page.getContent()
                                                .stream()
                                                .map(watchlistMapper::toResponse)
                                                .collect(Collectors.toList()))
                                .page(page.getNumber())
                                .size(page.getSize())
                                .totalElements(page.getTotalElements())
                                .totalPages(page.getTotalPages())
                                .first(page.isFirst())
                                .last(page.isLast())
                                .hasNext(page.hasNext())
                                .hasPrevious(page.hasPrevious())
                                .build();
        }
}