package com.stockasticappbackend.service.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.notification.NotificationResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Notification;
import com.stockasticappbackend.model.enums.NotificationType;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.stockasticappbackend.websocket.NotificationSessionRegistry notificationSessionRegistry;

    @Override
    public NotificationResponse createNotification(AppUser user, String title, String message, NotificationType type) {
        return createNotification(user, null, title, message, type);
    }

    @Override
    public NotificationResponse createNotification(AppUser user, Long stockId, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .stockId(stockId)
                .type(type)
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);

        NotificationResponse response = mapToResponse(notification);

        // Native WebSocket push (used by the frontend).
        try {
            notificationSessionRegistry.send(user.getEmail(), response);
        } catch (Exception e) {
            log.debug("Native notification push failed for {}: {}", user.getEmail(), e.getMessage());
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    response);
            log.info("Sent notification to user {}: {}", user.getEmail(), title);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to {}: {}", user.getEmail(), e.getMessage());
        }

        return response;
    }

    @Override
    public NotificationResponse createNotification(String userEmail, String title, String message, NotificationType type) {
        return createNotification(userEmail, null, title, message, type);
    }

    @Override
    public NotificationResponse createNotification(String userEmail, Long stockId, String title, String message, NotificationType type) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return createNotification(user, stockId, title, message, type);
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(String email, Pageable pageable) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Notification does not belong to user");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Notification> unread = notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    public long getUnreadCount(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Notification does not belong to user");
        }

        notificationRepository.delete(notification);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .stockId(n.getStockId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
