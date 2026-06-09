package com.stockasticappbackend.dto.notification;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockasticappbackend.model.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private Long stockId;
    private String title;
    private String message;
    private NotificationType type;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;
}
