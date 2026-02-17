package com.example.payment.controller;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.entity.Payment;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🌐 CONTROLLER - Point d'entrée de l'API REST
 *
 * Endpoints:
 * - POST /api/payments      → Traiter un paiement (Exactly Once)
 * - GET  /api/payments      → Liste tous les paiements
 * - GET  /api/payments/{id} → Récupère un paiement par ID
 * - GET  /api/payments/order/{orderId} → Récupère un paiement par orderId
 * - GET  /api/payments/user/{userId}   → Récupère les paiements d'un user
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 🔥 POST /api/payments - Traiter un paiement
     *
     * Exactly Once Processing:
     * - Si le paiement existe déjà → retourne 200 OK avec l'existant
     * - Si nouveau paiement → retourne 201 CREATED
     *
     * Exemple de requête:
     * {
     *   "orderId": "ORD-123",
     *   "amount": 100,
     *   "userId": "U-99"
     * }
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {
        LOGGER.info("📥 POST /api/payments - orderId: {}", paymentRequest.getOrderId());

        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Si le paiement existait déjà, retourner 200 OK (idempotence)
        if (response.isAlreadyProcessed()) {
            LOGGER.info("⚠️ Paiement déjà traité - retour 200 OK (Exactly Once)");
            return ResponseEntity.ok(response);
        }

        // Nouveau paiement créé
        LOGGER.info("✅ Nouveau paiement créé - retour 201 CREATED");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/payments/{id} - Récupérer un paiement par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        LOGGER.info("🔍 GET /api/payments/{}", id);
        Payment payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    /**
     * GET /api/payments/order/{orderId} - Récupérer un paiement par orderId
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        LOGGER.info("🔍 GET /api/payments/order/{}", orderId);
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(payment);
    }

    /**
     * GET /api/payments - Récupérer tous les paiements
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        LOGGER.info("📋 GET /api/payments");
        List<Payment> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * GET /api/payments/user/{userId} - Récupérer les paiements d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable String userId) {
        LOGGER.info("📋 GET /api/payments/user/{}", userId);
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        return ResponseEntity.ok(payments);
    }
}
