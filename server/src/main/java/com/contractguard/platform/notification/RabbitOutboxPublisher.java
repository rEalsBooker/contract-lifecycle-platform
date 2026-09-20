package com.contractguard.platform.notification;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "rabbit")
public class RabbitOutboxPublisher {
    private final OutboxMapper outbox;
    private final RabbitTemplate rabbitTemplate;

    public RabbitOutboxPublisher(OutboxMapper outbox, RabbitTemplate rabbitTemplate) {
        this.outbox = outbox; this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OutboxEventRow event) {
        if (outbox.claim(event.getId()) == 0) return;
        try {
            CorrelationData correlation = new CorrelationData(event.getId().toString());
            rabbitTemplate.convertAndSend(RabbitOutboxConfiguration.EXCHANGE, RabbitOutboxConfiguration.ROUTING_KEY, event, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(8, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) throw new IllegalStateException(confirm.getReason());
            outbox.markPublished(event.getId());
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "RabbitMQ publish failed" : exception.getMessage();
            outbox.markFailed(event.getId(), message.substring(0, Math.min(message.length(), 1000)));
        }
    }
}

