package com.example.payment.kafka.pipeline;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.kafka.pipeline.dto.OrderCreatedEvent;
import com.example.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "json")
public class OrderEventConsumerJson {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventConsumerJson.class);
    private final PaymentService paymentService;

    public OrderEventConsumerJson(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${app.kafka.topics.orders}", groupId = "order-processor-group-json", containerFactory = "pipelineJsonListenerContainerFactory")
    public void consumeOrderCreated(OrderCreatedEvent event) {
        LOGGER.info("📥 [JSON] Received OrderCreatedEvent for orderId: {}", event.getOrderId());

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(event.getOrderId());
        paymentRequest.setUserId(event.getUserId());
        paymentRequest.setAmount(event.getAmount());

        try {
            paymentService.processPayment(paymentRequest);
            LOGGER.info("✅ Successfully processed payment for orderId: {} from JSON event", event.getOrderId());
        } catch (Exception e) {
            LOGGER.error("❌ Failed to process payment for orderId: {}", event.getOrderId(), e);
        }
    }
}
