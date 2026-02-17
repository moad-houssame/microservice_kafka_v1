package com.example.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;  // ID de la commande (ex: "ORD-123") - UNIQUE pour idempotence

    @Column(name = "user_id", nullable = false)
    private String userId;   // ID de l'utilisateur (ex: "U-99")

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;  // Montant (ex: 100.00 MAD)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  // Statut du paiement

    @Column(nullable = false)
    private LocalDateTime createdAt;  // Date de création

    @Column
    private LocalDateTime updatedAt;  // Date de mise à jour

    // Enum pour les statuts de paiement
    public enum PaymentStatus {
        PENDING,    // En attente
        SUCCESS,    // Succès
        FAILED,     // Échec
        CANCELLED   // Annulé
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}