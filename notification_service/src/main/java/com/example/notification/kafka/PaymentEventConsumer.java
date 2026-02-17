package com.example.notification.kafka;

import com.example.notification.event.PaymentEvent;
import com.example.notification.service.NotificationService;
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
    public void consumePaymentEvent(PaymentEvent paymentEvent) {
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

        // Envoyer la notification
        notificationService.sendPaymentNotification(paymentEvent);
    }
}
