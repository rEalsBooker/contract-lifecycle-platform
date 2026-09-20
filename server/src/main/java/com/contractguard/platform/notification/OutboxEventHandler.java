package com.contractguard.platform.notification;

import com.contractguard.platform.ai.extraction.ContractParseEventPayload;
import com.contractguard.platform.ai.extraction.ContractParseWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OutboxEventHandler {
    private final NotificationMapper notifications;
    private final ObjectMapper objectMapper;
    private final ContractParseWorker contractParseWorker;

    public OutboxEventHandler(NotificationMapper notifications, ObjectMapper objectMapper,
                              ContractParseWorker contractParseWorker) {
        this.notifications = notifications; this.objectMapper = objectMapper; this.contractParseWorker = contractParseWorker;
    }

    public void handle(OutboxEventRow event) throws Exception {
        if ("NOTIFICATION_REQUESTED".equals(event.getEventType())) {
            NotificationEventPayload payload = objectMapper.readValue(event.getPayloadJson(), NotificationEventPayload.class);
            notifications.insert(event.getTenantId(), payload.recipientMembershipId(), payload.notificationType(),
                    payload.title(), payload.content(), payload.objectType(), payload.objectId(), payload.route(), payload.dedupKey());
        } else if ("CONTRACT_PARSE_REQUESTED".equals(event.getEventType())) {
            contractParseWorker.execute(objectMapper.readValue(event.getPayloadJson(), ContractParseEventPayload.class));
        } else {
            throw new IllegalArgumentException("Unsupported event type: " + event.getEventType());
        }
    }
}

