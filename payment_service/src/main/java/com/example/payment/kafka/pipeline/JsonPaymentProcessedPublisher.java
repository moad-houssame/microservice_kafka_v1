package com.example.payment.kafka.pipeline;

import com.example.payment.kafka.pipeline.dto.PaymentProcessedEvent;
import com.example.payment.observability.KafkaTracePropagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "json")
public class JsonPaymentProcessedPublisher implements PaymentProcessedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String paymentsTopic;

    public JsonPaymentProcessedPublisher(
            @Qualifier("pipelineJsonKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.payments}") String paymentsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentsTopic = paymentsTopic;
    }

    @Override
    public void publish(PaymentProcessedEvent event) {
        kafkaTemplate.send(
                KafkaTracePropagator.buildRecordWithCurrentTrace(
                        paymentsTopic,
                        event.getOrderId(),
                        event
                )
        );
    }
}
