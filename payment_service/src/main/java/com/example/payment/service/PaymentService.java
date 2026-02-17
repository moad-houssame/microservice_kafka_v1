package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Payment;

import java.util.List;

public interface PaymentService {

    /**
     * 🔥 Traite un paiement avec Exactly Once Processing
     * - Vérifie si le paiement existe déjà (idempotence)
     * - Sauvegarde en base PostgreSQL
     * - Publie l'événement sur Kafka
     * - Le tout dans une transaction atomique
     */
    PaymentResponse processPayment(PaymentRequest paymentRequest);

    /**
     * Récupère un paiement par son ID
     */
    Payment getPaymentById(Long id);

    /**
     * Récupère un paiement par son orderId
     */
    Payment getPaymentByOrderId(String orderId);

    /**
     * Récupère tous les paiements
     */
    List<Payment> getAllPayments();

    /**
     * Récupère tous les paiements d'un utilisateur
     */
    List<Payment> getPaymentsByUserId(String userId);
}
