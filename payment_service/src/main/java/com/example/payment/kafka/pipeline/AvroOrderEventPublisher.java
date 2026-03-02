package com.example.payment.kafka.pipeline;

import com.example.avro.OrderCreated;
import com.example.payment.kafka.pipeline.dto.OrderCreatedEvent;
import com.example.payment.observability.KafkaTracePropagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class AvroOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersTopic;

    public AvroOrderEventPublisher(
            @Qualifier("pipelineAvroKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.orders}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        OrderCreated avroEvent = OrderCreated.newBuilder()
                .setOrderId(event.getOrderId())
                .setUserId(event.getUserId())
                .setAmount(event.getAmount().doubleValue())
                .setCreatedAt(Instant.now().toString())
                .build();

        kafkaTemplate.executeInTransaction(ops -> {
            ops.send(
                    KafkaTracePropagator.buildRecordWithCurrentTrace(
                            ordersTopic,
                            event.getOrderId(),
                            avroEvent));
            return true;
        });
    }
}
