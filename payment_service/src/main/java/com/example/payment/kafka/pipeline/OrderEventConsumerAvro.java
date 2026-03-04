package com.example.payment.kafka.pipeline;

import com.example.avro.OrderCreated;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro", matchIfMissing = true)
public class OrderEventConsumerAvro {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderEventConsumerAvro.class);
    private final PaymentService paymentService;

    public OrderEventConsumerAvro(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${app.kafka.topics.orders}", groupId = "order-processor-group-avro", containerFactory = "pipelineAvroListenerContainerFactory")
    public void consumeOrderCreated(OrderCreated event) {
        LOGGER.info("📥 [AVRO] Received OrderCreatedEvent for orderId: {}", event.getOrderId());

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(event.getOrderId().toString());
        paymentRequest.setUserId(event.getUserId().toString());
        // Avro usually uses double or string for decimals depending on schema,
        // adjusting to string first
        paymentRequest.setAmount(new BigDecimal(String.valueOf(event.getAmount())));

        try {
            paymentService.processPayment(paymentRequest);
            LOGGER.info("✅ Successfully processed payment for orderId: {} from AVRO event", event.getOrderId());
        } catch (Exception e) {
            LOGGER.error("❌ Failed to process payment for orderId: {}", event.getOrderId(), e);
        }
    }
}
