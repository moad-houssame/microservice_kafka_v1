package com.example.payment.repository;

import com.example.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 🛡️ EXACTLY ONCE: Vérifie si un paiement existe déjà pour cet orderId
     * C'est la PREMIÈRE barrière contre les doublons
     */
    boolean existsByOrderId(String orderId);

    /**
     * Trouve un paiement par son orderId
     */
    Optional<Payment> findByOrderId(String orderId);

    /**
     * Trouve tous les paiements d'un utilisateur
     */
    List<Payment> findByUserId(String userId);

    /**
     * Trouve tous les paiements par statut
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);
}
