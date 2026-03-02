package com.example.payment.kafka.pipeline;

import com.example.payment.kafka.pipeline.dto.PaymentProcessedEvent;

public interface PaymentProcessedPublisher {
    void publish(PaymentProcessedEvent event);
}
