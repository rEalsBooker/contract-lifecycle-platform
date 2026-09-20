package com.contractguard.platform.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "rabbit")
public class RabbitOutboxDispatcher {
    private final OutboxMapper outbox;
    private final RabbitOutboxPublisher publisher;
    public RabbitOutboxDispatcher(OutboxMapper outbox, RabbitOutboxPublisher publisher) { this.outbox = outbox; this.publisher = publisher; }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
    public void dispatch() {
        outbox.recoverStale();
        for (OutboxEventRow event : outbox.findDue()) publisher.publish(event);
    }
}

