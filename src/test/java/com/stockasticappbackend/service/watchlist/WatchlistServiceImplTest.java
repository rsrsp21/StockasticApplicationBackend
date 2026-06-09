package com.stockasticappbackend.service.watchlist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.config.TestConfig;
import com.stockasticappbackend.dto.PageResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistCreateRequest;
import com.stockasticappbackend.dto.watchlist.WatchlistDetailResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistResponse;
import com.stockasticappbackend.dto.watchlist.WatchlistUpdateRequest;
import com.stockasticappbackend.exception.DuplicateResourceException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.WatchlistRepository;

@SpringBootTest
@Transactional
@Import(TestConfig.class)
class WatchlistServiceImplTest {

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;



    @Autowired
    private ActivityLogRepository activityLogRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        watchlistRepository.deleteAll();
        activityLogRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new AppUser();
        testUser.setEmail("watchlist@example.com");
        testUser.setName("Watchlist User");
        testUser.setPasswordHash("hash");
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);
    }

    @Test
    void createWatchlist_ShouldReturnResponse() {
        WatchlistCreateRequest request = WatchlistCreateRequest.builder()
                .name("Tech Stocks")
                .build();
        WatchlistResponse response = watchlistService.createWatchlist(testUser.getUserId(), request);
        assertNotNull(response);
        assertEquals("Tech Stocks", response.getName());
        assertNotNull(response.getId());
    }

    @Test
    void createWatchlist_DuplicateName_ShouldThrowException() {
        WatchlistCreateRequest request = WatchlistCreateRequest.builder()
                .name("My List")
                .build();
        watchlistService.createWatchlist(testUser.getUserId(), request);

        WatchlistCreateRequest duplicate = WatchlistCreateRequest.builder()
                .name("My List")
                .build();
        assertThrows(DuplicateResourceException.class, () -> {
            watchlistService.createWatchlist(testUser.getUserId(), duplicate);
        });
    }

    @Test
    void createWatchlist_UserNotFound_ShouldThrowException() {
        WatchlistCreateRequest request = WatchlistCreateRequest.builder()
                .name("Test")
                .build();
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistService.createWatchlist(999999L, request);
        });
    }

    @Test
    void getUserWatchlists_ShouldReturnAll() {
        watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("List 1").build());
        watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("List 2").build());
        watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("List 3").build());
        List<WatchlistResponse> result = watchlistService.getUserWatchlists(testUser.getUserId());
        assertEquals(3, result.size());
    }

    @Test
    void getUserWatchlists_Empty_ShouldReturnEmptyList() {
        List<WatchlistResponse> result = watchlistService.getUserWatchlists(testUser.getUserId());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserWatchlistsPaged_ShouldReturnPage() {
        for (int i = 0; i < 5; i++) {
            watchlistService.createWatchlist(testUser.getUserId(),
                    WatchlistCreateRequest.builder().name("List " + i).build());
        }
        PageResponse<WatchlistResponse> response = watchlistService.getUserWatchlistsPaged(
                testUser.getUserId(), 0, 2, "createdAt", "desc");
        assertEquals(2, response.getContent().size());
        assertEquals(5, response.getTotalElements());
        assertTrue(response.isHasNext());
    }

    @Test
    void getWatchlistById_ShouldReturnDetail() {
        WatchlistResponse created = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Detail Test").build());
        WatchlistDetailResponse detail = watchlistService.getWatchlistById(
                created.getId(), testUser.getUserId());
        assertNotNull(detail);
        assertEquals("Detail Test", detail.getName());
    }

    @Test
    void getWatchlistById_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistService.getWatchlistById(999999L, testUser.getUserId());
        });
    }

    @Test
    void getWatchlistById_WrongUser_ShouldThrowException() {
        WatchlistResponse created = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Private").build());
        AppUser otherUser = new AppUser();
        otherUser.setEmail("other@example.com");
        otherUser.setName("Other");
        otherUser.setPasswordHash("hash");
        otherUser = userRepository.save(otherUser);

        Long otherUserId = otherUser.getUserId();
        Long watchlistId = created.getId();
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistService.getWatchlistById(watchlistId, otherUserId);
        });
    }

    @Test
    void updateWatchlist_ShouldChangeName() {
        WatchlistResponse created = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Old Name").build());

        WatchlistUpdateRequest update = WatchlistUpdateRequest.builder()
                .name("New Name")
                .build();
        WatchlistResponse response = watchlistService.updateWatchlist(
                created.getId(), testUser.getUserId(), update);
        assertEquals("New Name", response.getName());
    }

    @Test
    void updateWatchlist_DuplicateName_ShouldThrowException() {
        watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Existing").build());
        WatchlistResponse second = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("To Rename").build());

        WatchlistUpdateRequest update = WatchlistUpdateRequest.builder()
                .name("Existing")
                .build();

        Long secondId = second.getId();
        Long userId = testUser.getUserId();
        assertThrows(DuplicateResourceException.class, () -> {
            watchlistService.updateWatchlist(secondId, userId, update);
        });
    }

    @Test
    void updateWatchlist_SameName_ShouldNotThrow() {
        WatchlistResponse created = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Keep Name").build());

        WatchlistUpdateRequest update = WatchlistUpdateRequest.builder()
                .name("Keep Name")
                .build();
        WatchlistResponse response = watchlistService.updateWatchlist(
                created.getId(), testUser.getUserId(), update);
        assertEquals("Keep Name", response.getName());
    }

    @Test
    void deleteWatchlist_ShouldRemove() {
        WatchlistResponse created = watchlistService.createWatchlist(testUser.getUserId(),
                WatchlistCreateRequest.builder().name("Delete Me").build());
        watchlistService.deleteWatchlist(created.getId(), testUser.getUserId());
        List<WatchlistResponse> remaining = watchlistService.getUserWatchlists(testUser.getUserId());
        assertTrue(remaining.isEmpty());
    }

    @Test
    void deleteWatchlist_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            watchlistService.deleteWatchlist(999999L, testUser.getUserId());
        });
    }
}

