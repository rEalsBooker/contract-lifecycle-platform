package com.contractguard.platform.notification;

import com.contractguard.platform.ai.extraction.ContractParseWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxEventHandlerTest {
    @Test
    void materializesNotificationPayload() throws Exception {
        NotificationMapper notifications = mock(NotificationMapper.class);
        ContractParseWorker worker = mock(ContractParseWorker.class);
        NotificationEventPayload payload = new NotificationEventPayload(20L, "EVIDENCE_PENDING_REVIEW",
                "待审核", "任务材料已提交", "TASK", 30L, "/tasks?taskId=30", "TASK:30:ROUND:1");
        OutboxEventRow event = new OutboxEventRow();
        event.setId(7L);
        event.setTenantId(100L);
        event.setEventType("NOTIFICATION_REQUESTED");
        event.setPayloadJson(new ObjectMapper().writeValueAsString(payload));

        new OutboxEventHandler(notifications, new ObjectMapper(), worker).handle(event);

        verify(notifications).insert(100L, 20L, "EVIDENCE_PENDING_REVIEW", "待审核", "任务材料已提交",
                "TASK", 30L, "/tasks?taskId=30", "TASK:30:ROUND:1");
    }
}

