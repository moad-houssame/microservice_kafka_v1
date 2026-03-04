package com.example.payment.service.impl;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.exception.ResourceNotFoundException;
import com.example.payment.kafka.event.PaymentEvent;
import com.example.payment.outbox.OutboxService;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;

/**
 * 🔥 SERVICE PRINCIPAL - Cœur du système de paiement
 *
 * Implémente Exactly Once Processing via :
 * 1. Vérification d'idempotence (existsByOrderId)
 * 2. Contrainte UNIQUE en base de données
 * 3. Transaction atomique DB + outbox
 * 4. Publication Kafka via outbox
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final Counter paymentsProcessedCounter;

    public PaymentServiceImpl(PaymentRepository paymentRepository, OutboxService outboxService,
            MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.outboxService = outboxService;
        this.paymentsProcessedCounter = Counter.builder("payments_processed_total")
                .description("Total number of payments successfully processed")
                .register(meterRegistry);
    }

    /**
     * 🔥 TRAITEMENT DU PAIEMENT - Exactly Once Processing
     *
     * Flux:
     * 1. Vérifier si paiement existe déjà (IDEMPOTENCE)
     * 2. Créer et sauvegarder le paiement (TRANSACTION)
     * 3. Publier l'événement Kafka (TRANSACTION)
     *
     * @param paymentRequest Les données du paiement
     * @return PaymentResponse avec le résultat
     */
    @Override
    @Transactional("transactionManager") // Transaction atomique DB + outbox
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        String orderId = paymentRequest.getOrderId();

        LOGGER.info("📥 Réception demande de paiement pour orderId: {}", orderId);

        // ========================================
        // ÉTAPE 1: VÉRIFICATION IDEMPOTENCE
        // 🛡️ PREMIÈRE BARRIÈRE EXACTLY ONCE
        // ========================================
        if (paymentRepository.existsByOrderId(orderId)) {
            LOGGER.warn("⚠️ Paiement déjà existant pour orderId: {} - Exactly Once en action!", orderId);
            Payment existingPayment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

            // Retourne le paiement existant sans le traiter à nouveau
            return PaymentResponse.alreadyExists(existingPayment);
        }

        // ========================================
        // ÉTAPE 2: CRÉATION DU PAIEMENT
        // ========================================
        LOGGER.info("💳 Création du paiement pour orderId: {}", orderId);

        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(paymentRequest.getUserId())
                .amount(paymentRequest.getAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();

        // ========================================
        // ÉTAPE 3: SAUVEGARDE EN BASE POSTGRESQL
        // 🛡️ DEUXIÈME BARRIÈRE: Contrainte UNIQUE
        // ========================================
        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("⚠️ Contrainte UNIQUE violée pour orderId: {} - retour paiement existant", orderId);
            Payment existingPayment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
            return PaymentResponse.alreadyExists(existingPayment);
        }
        LOGGER.info("✅ Paiement sauvegardé en base - ID: {}, OrderId: {}",
                savedPayment.getId(), savedPayment.getOrderId());

        // ========================================
        // ÉTAPE 4: MISE À JOUR DU STATUT À SUCCESS
        // ========================================
        savedPayment.setStatus(Payment.PaymentStatus.SUCCESS);
        savedPayment = paymentRepository.save(savedPayment);
        LOGGER.info("✅ Statut mis à jour: SUCCESS pour orderId: {}", orderId);

        // ========================================
        // ÉTAPE 5: ENQUEUE OUTBOX POUR KAFKA
        // ========================================
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .paymentId(savedPayment.getId())
                .orderId(savedPayment.getOrderId())
                .userId(savedPayment.getUserId())
                .amount(savedPayment.getAmount())
                .status(savedPayment.getStatus().name())
                .build();

        outboxService.enqueuePaymentEvent(paymentEvent);
        LOGGER.info("Outbox event queued for paymentId: {}", savedPayment.getId());

        // ========================================
        // ÉTAPE 6: RETOUR DE LA RÉPONSE
        // ========================================
        LOGGER.info("🎉 Paiement traité avec succès - Exactly Once garanti!");
        paymentsProcessedCounter.increment();
        return PaymentResponse.success(savedPayment);
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public Payment getPaymentById(Long id) {
        LOGGER.info("🔍 Recherche du paiement par ID: {}", id);
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id.toString()));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public Payment getPaymentByOrderId(String orderId) {
        LOGGER.info("🔍 Recherche du paiement par orderId: {}", orderId);
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<Payment> getAllPayments() {
        LOGGER.info("📋 Récupération de tous les paiements");
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true, transactionManager = "transactionManager")
    public List<Payment> getPaymentsByUserId(String userId) {
        LOGGER.info("📋 Récupération des paiements pour userId: {}", userId);
        return paymentRepository.findByUserId(userId);
    }
}
