package com.stockasticappbackend.service.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.stockasticappbackend.dto.notification.NotificationResponse;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.NotificationType;

public interface NotificationService {

    NotificationResponse createNotification(AppUser user, String title, String message, NotificationType type);
    NotificationResponse createNotification(AppUser user, Long stockId, String title, String message, NotificationType type);

    NotificationResponse createNotification(String userEmail, String title, String message, NotificationType type);
    NotificationResponse createNotification(String userEmail, Long stockId, String title, String message, NotificationType type);

    Page<NotificationResponse> getUserNotifications(String email, Pageable pageable);

    List<NotificationResponse> getUnreadNotifications(String email);

    void markAsRead(Long notificationId, String email);

    void markAllAsRead(String email);

    long getUnreadCount(String email);

    void deleteNotification(Long notificationId, String email);
}
