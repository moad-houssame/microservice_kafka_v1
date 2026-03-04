package com.example.order.controller;

import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 🌐 CONTROLLER - Point d'entrée de l'API REST Order
 *
 * Endpoints:
 * - POST /api/orders → Créer une commande (Idempotent)
 * - GET /api/orders → Liste toutes les commandes
 * - GET /api/orders/{id} → Récupère une commande par ID
 * - GET /api/orders/order/{orderId} → Récupère une commande par orderId
 * - GET /api/orders/user/{userId} → Récupère les commandes d'un user
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final Counter ordersCreatedCounter;

    public OrderController(OrderService orderService, OrderRepository orderRepository, MeterRegistry meterRegistry) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.ordersCreatedCounter = Counter.builder("orders_processed_total")
                .description("Total number of distinct orders created")
                .register(meterRegistry);
    }

    /**
     * 🔥 POST /api/orders - Créer une commande (Idempotent)
     *
     * - Si la commande existe déjà → retourne 409 CONFLICT
     * - Si nouvelle commande → retourne 202 ACCEPTED
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest request) {
        LOGGER.info("📥 POST /api/orders - orderId: {}", request.orderId);

        OrderService.OrderResult result = orderService.createOrder(
                request.orderId, request.userId, request.amount);

        String traceId = Span.current().getSpanContext().getTraceId();
        String spanId = Span.current().getSpanContext().getSpanId();

        if (result.alreadyExisted()) {
            LOGGER.info("⚠️ Commande déjà créée - retour 409 CONFLICT (Idempotence)");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header("X-Trace-Id", traceId)
                    .header("X-Span-Id", spanId)
                    .body("{\"message\": \"Order already exists for orderId: " + request.orderId + "\"}");
        }

        ordersCreatedCounter.increment();
        LOGGER.info("✅ Nouvelle commande créée - retour 202 ACCEPTED");
        return ResponseEntity.accepted()
                .header("X-Trace-Id", traceId)
                .header("X-Span-Id", spanId)
                .build();
    }

    /**
     * 📋 GET /api/orders - Récupérer toutes les commandes
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        LOGGER.info("📋 GET /api/orders");
        List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    /**
     * 🔍 GET /api/orders/{id} - Récupérer une commande par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        LOGGER.info("🔍 GET /api/orders/{}", id);
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🔍 GET /api/orders/order/{orderId} - Récupérer une commande par orderId
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Order> getOrderByOrderId(@PathVariable String orderId) {
        LOGGER.info("🔍 GET /api/orders/order/{}", orderId);
        return orderRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 📋 GET /api/orders/user/{userId} - Récupérer les commandes d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable String userId) {
        LOGGER.info("📋 GET /api/orders/user/{}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    public static class OrderRequest {
        @NotBlank
        public String orderId;
        @NotBlank
        public String userId;
        @NotNull
        @DecimalMin("0.01")
        public BigDecimal amount;
    }
}
