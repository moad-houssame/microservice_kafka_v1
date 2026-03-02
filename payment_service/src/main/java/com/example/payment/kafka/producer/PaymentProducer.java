package com.example.payment.kafka.producer;

import com.example.payment.kafka.event.PaymentEvent;
import com.example.payment.observability.KafkaTracePropagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 📤 PRODUCER KAFKA - Publication des événements de paiement
 *
 * Publie les PaymentEvent sur le topic "payment-topic"
 * Le Notification Service consomme ces événements pour envoyer les notifications
 */
@Service
public class PaymentProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentProducer.class);
    private static final String TOPIC = "payment-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 📤 Envoie un événement de paiement sur Kafka
     *
     * Utilise l'orderId comme clé pour garantir l'ordre des messages
     * d'une même commande dans la même partition
     *
     * @param paymentEvent L'événement à publier
     */
    public void sendPaymentEvent(PaymentEvent paymentEvent) {
        LOGGER.info("📤 Envoi événement Kafka - Topic: {}, OrderId: {}", TOPIC, paymentEvent.getOrderId());

        // Utilise orderId comme clé pour le partitioning
        String key = paymentEvent.getOrderId();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                KafkaTracePropagator.buildRecordWithCurrentTrace(TOPIC, key, paymentEvent)
        );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                LOGGER.info("✅ Événement Kafka envoyé avec succès!");
                LOGGER.info("   📍 Topic: {}", result.getRecordMetadata().topic());
                LOGGER.info("   📍 Partition: {}", result.getRecordMetadata().partition());
                LOGGER.info("   📍 Offset: {}", result.getRecordMetadata().offset());
                LOGGER.info("   📍 PaymentId: {}", paymentEvent.getPaymentId());
            } else {
                LOGGER.error("❌ Erreur lors de l'envoi Kafka: {}", ex.getMessage());
            }
        });
    }
}
