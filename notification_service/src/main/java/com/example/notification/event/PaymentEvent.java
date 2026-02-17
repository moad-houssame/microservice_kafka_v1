package com.example.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 📦 Événement de paiement reçu de Kafka
 *
 * Doit correspondre au PaymentEvent du Payment Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private Long paymentId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
}
