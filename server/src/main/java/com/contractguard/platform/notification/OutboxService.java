package com.contractguard.platform.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class OutboxService {
    private final OutboxMapper outbox;
    private final NotificationMapper notifications;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxMapper outbox, NotificationMapper notifications, ObjectMapper objectMapper) {
        this.outbox = outbox;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    public void enqueueNotification(Long tenantId, Long recipientMembershipId, String notificationType,
                                    String title, String content, String objectType, Long objectId,
                                    String route, String dedupKey) {
        NotificationEventPayload payload = new NotificationEventPayload(recipientMembershipId, notificationType,
                title, content, objectType, objectId, route, dedupKey);
        String eventKey = "NOTIFY:" + recipientMembershipId + ":" + dedupKey;
        enqueueEvent(tenantId, eventKey, "NOTIFICATION_REQUESTED", objectType, objectId, payload);
    }

    public void enqueueOverdueRisk(Long tenantId, Long riskId, Long contractId, String contractNo,
                                   String factSummary) {
        String dailyKey = "RISK_OVERDUE:" + riskId + ":" + LocalDate.now();
        for (Long recipient : notifications.findFinancialRiskRecipients(tenantId)) {
            enqueueNotification(tenantId, recipient, "RISK_OVERDUE", "新增高风险：回款逾期",
                    contractNo + " · " + factSummary, "RISK", riskId,
                    "/risks?contractId=" + contractId, dailyKey);
        }
    }

    public void enqueueEvent(Long tenantId, String eventKey, String eventType, String aggregateType,
                             Long aggregateId, Object payload) {
        outbox.insert(tenantId, eventKey, eventType, aggregateType, aggregateId, json(payload));
    }

    private String json(Object payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("通知事件序列化失败", exception); }
    }
}

