package com.example.payment.outbox;

import com.example.payment.kafka.event.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "payment-topic";
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    @Transactional("transactionManager")
    public void publish() {
        List<OutboxEvent> pending = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        List<OutboxEvent> retry = outboxEventRepository
                .findTop50ByStatusAndAttemptsLessThanOrderByCreatedAtAsc(
                        OutboxEventStatus.FAILED,
                        MAX_ATTEMPTS
                );

        pending.addAll(retry);

        for (OutboxEvent event : pending) {
            try {
                PaymentEvent paymentEvent = objectMapper.readValue(event.getPayload(), PaymentEvent.class);
                String key = paymentEvent.getOrderId();

                kafkaTemplate.send(TOPIC, key, paymentEvent).get(10, TimeUnit.SECONDS);

                event.setStatus(OutboxEventStatus.SENT);
                event.setSentAt(LocalDateTime.now());
                LOGGER.info("Outbox event sent: id={}, aggregateId={}", event.getId(), event.getAggregateId());
            } catch (Exception ex) {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setAttempts(event.getAttempts() + 1);
                LOGGER.warn("Outbox send failed: id={}, attempts={}, error={}",
                        event.getId(), event.getAttempts(), ex.getMessage());
            }
        }

        outboxEventRepository.saveAll(pending);
    }
}
