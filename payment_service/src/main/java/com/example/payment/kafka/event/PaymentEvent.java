package com.example.payment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 📦 Événement Kafka pour les paiements
 *
 * Cet événement est publié sur le topic "payment-topic" après chaque paiement réussi.
 * Il est consommé par le Notification Service pour envoyer les notifications.
 *
 * Exemple de payload:
 * {
 *   "paymentId": 1,
 *   "orderId": "ORD-123",
 *   "userId": "U-99",
 *   "amount": 100.00,
 *   "status": "SUCCESS",
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private Long paymentId;      // ID du paiement en base
    private String orderId;      // ID de la commande (pour traçabilité)
    private String userId;       // ID de l'utilisateur (pour notification)
    private BigDecimal amount;   // Montant du paiement
    private String status;       // Statut (SUCCESS, FAILED, etc.)

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();  // Horodatage de l'événement

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "paymentId=" + paymentId +
                ", orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
