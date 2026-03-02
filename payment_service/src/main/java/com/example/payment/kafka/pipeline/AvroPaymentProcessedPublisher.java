package com.example.payment.kafka.pipeline;

import com.example.avro.PaymentProcessed;
import com.example.payment.kafka.pipeline.dto.PaymentProcessedEvent;
import com.example.payment.observability.KafkaTracePropagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class AvroPaymentProcessedPublisher implements PaymentProcessedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentsTopic;

    public AvroPaymentProcessedPublisher(
            @Qualifier("pipelineAvroKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.payments}") String paymentsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentsTopic = paymentsTopic;
    }

    @Override
    public void publish(PaymentProcessedEvent event) {
        PaymentProcessed avroEvent = PaymentProcessed.newBuilder()
                .setOrderId(event.getOrderId())
                .setUserId(event.getUserId())
                .setAmount(event.getAmount().doubleValue())
                .setStatus(event.getStatus())
                .setProcessedAt(Instant.now().toString())
                .build();

        kafkaTemplate.executeInTransaction(ops -> {
            ops.send(
                    KafkaTracePropagator.buildRecordWithCurrentTrace(
                            paymentsTopic,
                            event.getOrderId(),
                            avroEvent));
            return true;
        });
    }
}
