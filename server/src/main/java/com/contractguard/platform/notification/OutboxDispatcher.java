package com.contractguard.platform.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "database", matchIfMissing = true)
public class OutboxDispatcher {
    private final OutboxMapper outbox;
    private final OutboxEventProcessor processor;

    public OutboxDispatcher(OutboxMapper outbox, OutboxEventProcessor processor) {
        this.outbox = outbox;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
    public void dispatch() {
        outbox.recoverStale();
        for (OutboxEventRow event : outbox.findDue()) processor.process(event);
    }
}

