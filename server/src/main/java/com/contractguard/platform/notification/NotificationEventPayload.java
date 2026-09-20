package com.contractguard.platform.notification;

public record NotificationEventPayload(Long recipientMembershipId, String notificationType, String title,
                                       String content, String objectType, Long objectId,
                                       String route, String dedupKey) { }

