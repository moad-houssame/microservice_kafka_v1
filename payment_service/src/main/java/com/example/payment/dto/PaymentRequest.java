package com.example.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la requête de paiement
 * Exemple de requête:
 * {
 *   "orderId": "ORD-123",
 *   "amount": 100,
 *   "userId": "U-99"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "L'orderId est obligatoire")
    private String orderId;  // ID unique de la commande

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal amount;  // Montant du paiement en MAD

    @NotBlank(message = "L'userId est obligatoire")
    private String userId;  // ID de l'utilisateur qui paie
}
