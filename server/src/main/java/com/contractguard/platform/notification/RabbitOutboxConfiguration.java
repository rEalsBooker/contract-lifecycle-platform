package com.contractguard.platform.notification;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "rabbit")
public class RabbitOutboxConfiguration {
    public static final String EXCHANGE = "contract.lifecycle.events";
    public static final String QUEUE = "contract.lifecycle.outbox";
    public static final String ROUTING_KEY = "contract.lifecycle.event";
    public static final String DEAD_LETTER_EXCHANGE = "contract.lifecycle.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "contract.lifecycle.outbox.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "contract.lifecycle.dead";

    @Bean TopicExchange contractEventExchange() { return new TopicExchange(EXCHANGE, true, false); }
    @Bean DirectExchange contractDeadLetterExchange() { return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false); }
    @Bean Queue contractEventQueue() { return QueueBuilder.durable(QUEUE)
            .deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY).build(); }
    @Bean Queue contractDeadLetterQueue() { return QueueBuilder.durable(DEAD_LETTER_QUEUE).build(); }
    @Bean Binding contractEventBinding(@Qualifier("contractEventQueue") Queue contractEventQueue,
                                       TopicExchange contractEventExchange) {
        return BindingBuilder.bind(contractEventQueue).to(contractEventExchange).with(ROUTING_KEY);
    }
    @Bean Binding contractDeadLetterBinding(@Qualifier("contractDeadLetterQueue") Queue contractDeadLetterQueue,
                                            DirectExchange contractDeadLetterExchange) {
        return BindingBuilder.bind(contractDeadLetterQueue).to(contractDeadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }
    @Bean Jackson2JsonMessageConverter rabbitJsonMessageConverter() { return new Jackson2JsonMessageConverter(); }
}

