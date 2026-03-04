package com.example.order.kafka;

import com.example.avro.OrderCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventPublisher.class);
    private final KafkaTemplate<String, OrderCreated> kafkaTemplate;

    @Value("${app.kafka.topics.orders}")
    private String ordersTopic;

    public OrderEventPublisher(KafkaTemplate<String, OrderCreated> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreatedEvent(com.example.order.entity.Order order) {
        OrderCreated event = OrderCreated.newBuilder()
                .setOrderId(order.getOrderId())
                .setUserId(order.getUserId())
                .setAmount(order.getAmount().doubleValue())
                .setCreatedAt(LocalDateTime.now().toString())
                .build();

        LOGGER.info("Publishing OrderCreated event for orderId: {}", order.getOrderId());
        kafkaTemplate.send(ordersTopic, order.getOrderId(), event);
    }
}
