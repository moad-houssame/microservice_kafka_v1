package com.example.notification.kafka;

import com.example.notification.event.PaymentEvent;
import com.example.notification.observability.KafkaTracePropagator;
import com.example.notification.service.NotificationService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 📥 CONSUMER KAFKA - Écoute les événements de paiement
 *
 * Consomme les messages du topic "payment-topic" et déclenche
 * l'envoi des notifications via le NotificationService
 */
@Component
public class PaymentEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentEventConsumer.class);
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("notification-service");

    private final NotificationService notificationService;

    public PaymentEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 🎧 Écoute le topic "payment-topic"
     *
     * Chaque fois qu'un paiement est effectué, cet événement est reçu
     * et une notification est envoyée à l'utilisateur
     */
    @KafkaListener(
            topics = "payment-topic",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(ConsumerRecord<String, PaymentEvent> record) {
        PaymentEvent paymentEvent = record.value();

        Context extractedContext = KafkaTracePropagator.extractContext(record.headers());
        Span consumeSpan = TRACER.spanBuilder("kafka.consume payment-topic")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(extractedContext)
                .startSpan();

        try (Scope scope = consumeSpan.makeCurrent()) {
            LOGGER.info("========================================");
            LOGGER.info("📥 ÉVÉNEMENT KAFKA REÇU!");
            LOGGER.info("========================================");
            LOGGER.info("   📍 PaymentId: {}", paymentEvent.getPaymentId());
            LOGGER.info("   📍 OrderId: {}", paymentEvent.getOrderId());
            LOGGER.info("   📍 UserId: {}", paymentEvent.getUserId());
            LOGGER.info("   📍 Amount: {} MAD", paymentEvent.getAmount());
            LOGGER.info("   📍 Status: {}", paymentEvent.getStatus());
            LOGGER.info("   📍 Timestamp: {}", paymentEvent.getTimestamp());
            LOGGER.info("========================================");

            Span notificationSpan = TRACER.spanBuilder("notification.send")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();
            try (Scope notificationScope = notificationSpan.makeCurrent()) {
                notificationService.sendPaymentNotification(paymentEvent);
            } finally {
                notificationSpan.end();
            }
        } finally {
            consumeSpan.end();
        }
    }
}
