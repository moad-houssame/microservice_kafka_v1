package com.example.payment.kafka.pipeline;

import com.example.payment.kafka.pipeline.dto.OrderCreatedEvent;
import com.example.payment.observability.KafkaTracePropagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "json")
public class JsonOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersTopic;

    public JsonOrderEventPublisher(
            @Qualifier("pipelineJsonKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.orders}") String ordersTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        kafkaTemplate.send(
                KafkaTracePropagator.buildRecordWithCurrentTrace(
                        ordersTopic,
                        event.getOrderId(),
                        event
                )
        );
    }
}
