package com.stockasticappbackend.service.notification;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.notification.NotificationResponse;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Notification;
import com.stockasticappbackend.model.enums.NotificationType;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.NotificationRepository;
import com.stockasticappbackend.service.activity.ActivityLogInternalService;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AppUserRepository userRepository;
    
    @Autowired
    private ActivityLogRepository activityLogRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @MockitoBean
    private ActivityLogInternalService activityLogService;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        activityLogRepository.deleteAll();
        notificationRepository.deleteAll();
        String uniqueEmail = "notify-test-" + System.currentTimeMillis() + "@example.com";
        testUser = new AppUser();
        testUser.setEmail(uniqueEmail);
        testUser.setPasswordHash("password");
        testUser.setName("Test User");
        testUser = userRepository.save(testUser);
    }

    @Test
    void createNotification_ShouldSaveAndBroadcast() {
        NotificationResponse response = notificationService.createNotification(
                testUser, "Test Title", "Test Message", NotificationType.INFO);
        assertNotNull(response);
        assertNotNull(response.getNotificationId());
        assertEquals("Test Title", response.getTitle());
        List<Notification> all = notificationRepository.findAll();
        assertEquals(1, all.size());
        assertEquals("Test Message", all.get(0).getMessage());
        verify(messagingTemplate).convertAndSendToUser(
                eq(testUser.getEmail()), 
                eq("/queue/notifications"), 
                any(NotificationResponse.class));
    }

    @Test
    void createNotification_ByEmail_ShouldWork() {
        NotificationResponse response = notificationService.createNotification(
                testUser.getEmail(), "Email Title", "Email Msg", NotificationType.ALERT);
        assertNotNull(response);
        assertEquals("Email Title", response.getTitle());
    }
    
    @Test
    void getUserNotifications_ShouldReturnPage() {
        createNotification("Title 1");
        createNotification("Title 2");
        createNotification("Title 3");
        Page<NotificationResponse> page = notificationService.getUserNotifications(
                testUser.getEmail(), PageRequest.of(0, 2));
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals("Title 3", page.getContent().get(0).getTitle());
    }

    @Test
    void markAsRead_ShouldUpdateStatus() {
        Notification n = createNotification("Unread");
        notificationService.markAsRead(n.getNotificationId(), testUser.getEmail());
        Notification updated = notificationRepository.findById(n.getNotificationId()).orElseThrow();
        assertTrue(updated.isRead());
    }
    
    @Test
    void markAllAsRead_ShouldUpdateAll() {
        createNotification("N1");
        createNotification("N2");
        
        assertEquals(2, notificationService.getUnreadCount(testUser.getEmail()));
        notificationService.markAllAsRead(testUser.getEmail());
        assertEquals(0, notificationService.getUnreadCount(testUser.getEmail()));
        assertTrue(notificationRepository.findAll().stream().allMatch(Notification::isRead));
    }
    
    @Test
    void deleteNotification_ShouldRemove() {
        Notification n = createNotification("To Delete");
        notificationService.deleteNotification(n.getNotificationId(), testUser.getEmail());
        assertTrue(notificationRepository.findById(n.getNotificationId()).isEmpty());
    }
    
    @Test
    void deleteNotification_WrongUser_ShouldThrow() {
        Notification n = createNotification("Mine");
        assertThrows(IllegalArgumentException.class, () -> 
            notificationService.deleteNotification(n.getNotificationId(), "other@example.com")
        );
    }
    
    @Test
    void getUnreadNotifications_ShouldReturnOnlyUnread() {
        createNotification("Unread 1");
        Notification n2 = createNotification("Read 1");
        
        notificationService.markAsRead(n2.getNotificationId(), testUser.getEmail());
        List<NotificationResponse> unread = notificationService.getUnreadNotifications(testUser.getEmail());
        assertEquals(1, unread.size());
        assertEquals("Unread 1", unread.get(0).getTitle());
    }
    private Notification createNotification(String title) {
        Notification n = Notification.builder()
                .user(testUser)
                .title(title)
                .message("Msg")
                .type(NotificationType.INFO)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(n);
    }
}


