package com.contractguard.platform.notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String notificationType, String title, String content,
                                   String objectType, Long objectId, String route,
                                   boolean unread, LocalDateTime createdAt) {
    public static NotificationResponse from(NotificationRow row) {
        return new NotificationResponse(row.getId(), row.getNotificationType(), row.getTitle(), row.getContent(),
                row.getObjectType(), row.getObjectId(), row.getRoute(), row.getReadAt() == null, row.getCreatedAt());
    }
}

