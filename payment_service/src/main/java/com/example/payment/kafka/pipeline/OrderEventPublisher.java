package com.example.payment.kafka.pipeline;

import com.example.payment.kafka.pipeline.dto.OrderCreatedEvent;

public interface OrderEventPublisher {
    void publish(OrderCreatedEvent event);
}
