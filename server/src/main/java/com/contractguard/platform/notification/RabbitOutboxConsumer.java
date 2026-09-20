package com.contractguard.platform.notification;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "rabbit")
public class RabbitOutboxConsumer {
    private final OutboxEventHandler handler;
    public RabbitOutboxConsumer(OutboxEventHandler handler) { this.handler = handler; }

    @RabbitListener(queues = RabbitOutboxConfiguration.QUEUE)
    public void consume(OutboxEventRow event) throws Exception { handler.handle(event); }
}

