package com.contractguard.platform.notification;

import org.springframework.stereotype.Service;

@Service
public class OutboxEventProcessor {
    private final OutboxMapper outbox;
    private final OutboxEventHandler handler;

    public OutboxEventProcessor(OutboxMapper outbox, OutboxEventHandler handler) {
        this.outbox = outbox;
        this.handler = handler;
    }

    public void process(OutboxEventRow event) {
        if (outbox.claim(event.getId()) == 0) return;
        try {
            handler.handle(event);
            outbox.markPublished(event.getId());
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            outbox.markFailed(event.getId(), message.substring(0, Math.min(message.length(), 1000)));
        }
    }
}

