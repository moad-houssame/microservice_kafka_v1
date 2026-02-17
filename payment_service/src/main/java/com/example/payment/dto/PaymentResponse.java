package com.example.payment.dto;

import com.example.payment.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse après traitement du paiement
 * Contient toutes les informations du paiement traité
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private String message;
    private LocalDateTime timestamp;
    private boolean alreadyProcessed;  // Indique si le paiement existait déjà

    /**
     * Crée une réponse à partir d'une entité Payment
     */
    public static PaymentResponse fromPayment(Payment payment, String message, boolean alreadyProcessed) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .message(message)
                .timestamp(LocalDateTime.now())
                .alreadyProcessed(alreadyProcessed)
                .build();
    }

    /**
     * Réponse pour un nouveau paiement créé avec succès
     */
    public static PaymentResponse success(Payment payment) {
        return fromPayment(payment, "Paiement traité avec succès", false);
    }

    /**
     * Réponse quand le paiement existe déjà (idempotence)
     */
    public static PaymentResponse alreadyExists(Payment payment) {
        return fromPayment(payment, "Paiement déjà traité (Exactly Once)", true);
    }
}
