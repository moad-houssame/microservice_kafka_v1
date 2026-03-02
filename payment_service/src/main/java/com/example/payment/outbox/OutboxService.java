package com.example.payment.outbox;

import com.example.payment.kafka.event.PaymentEvent;
import com.example.payment.observability.KafkaTracePropagator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void enqueuePaymentEvent(PaymentEvent paymentEvent) {
        String payload = toJson(paymentEvent);
        String traceParent = KafkaTracePropagator.currentTraceParent();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Payment")
                .aggregateId(String.valueOf(paymentEvent.getPaymentId()))
                .eventType("PaymentCreated")
                .payload(payload)
                .status(OutboxEventStatus.NEW)
                .attempts(0)
                .traceParent(traceParent)
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    private String toJson(PaymentEvent paymentEvent) {
        try {
            return objectMapper.writeValueAsString(paymentEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize PaymentEvent", e);
        }
    }
}
