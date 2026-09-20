package com.contractguard.platform.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxEventProcessorTest {
    @Test
    void materializesNotificationAndMarksEventPublished() throws Exception {
        OutboxMapper outbox = mock(OutboxMapper.class);
        NotificationMapper notifications = mock(NotificationMapper.class);
        ObjectMapper objectMapper = new ObjectMapper();
        NotificationEventPayload payload = new NotificationEventPayload(20L, "EVIDENCE_PENDING_REVIEW",
                "待审核", "任务材料已提交", "TASK", 30L, "/tasks?taskId=30", "TASK:30:ROUND:1");
        OutboxEventRow event = event(7L, 100L, "NOTIFICATION_REQUESTED", objectMapper.writeValueAsString(payload));
        when(outbox.claim(7L)).thenReturn(1);

        OutboxEventHandler handler = mock(OutboxEventHandler.class);
        new OutboxEventProcessor(outbox, handler).process(event);

        verify(handler).handle(event);
        verify(outbox).markPublished(7L);
    }

    @Test
    void unsupportedEventIsRetriedInsteadOfSilentlyLost() throws Exception {
        OutboxMapper outbox = mock(OutboxMapper.class);
        when(outbox.claim(8L)).thenReturn(1);

        OutboxEventHandler handler = mock(OutboxEventHandler.class);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Unsupported event type: UNKNOWN")).when(handler).handle(org.mockito.ArgumentMatchers.any());
        new OutboxEventProcessor(outbox, handler).process(event(8L, 100L, "UNKNOWN", "{}"));

        verify(outbox).markFailed(8L, "Unsupported event type: UNKNOWN");
    }

    private OutboxEventRow event(Long id, Long tenantId, String type, String payload) {
        OutboxEventRow row = new OutboxEventRow();
        row.setId(id);
        row.setTenantId(tenantId);
        row.setEventType(type);
        row.setPayloadJson(payload);
        return row;
    }
}

