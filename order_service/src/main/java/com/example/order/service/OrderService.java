package com.example.order.service;

import com.example.order.entity.Order;
import com.example.order.kafka.OrderEventPublisher;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    /**
     * Result record to communicate whether the order was newly created or already
     * existed.
     */
    public record OrderResult(Order order, boolean alreadyExisted) {
    }

    @Transactional
    public OrderResult createOrder(String orderId, String userId, BigDecimal amount) {
        LOGGER.info("Received request to create orderId: {}", orderId);

        if (orderRepository.existsByOrderId(orderId)) {
            LOGGER.warn("Order already exists for orderId: {} - Idempotency hit!", orderId);
            Order existing = orderRepository.findByOrderId(orderId).orElseThrow();
            return new OrderResult(existing, true);
        }

        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(Order.OrderStatus.CREATED)
                .build();

        Order savedOrder;
        try {
            savedOrder = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            LOGGER.warn("UNIQUE constraint violated for orderId: {} - returning existing order", orderId);
            Order existing = orderRepository.findByOrderId(orderId).orElseThrow();
            return new OrderResult(existing, true);
        }

        LOGGER.info("Order saved in DB. Publishing via Kafka...");
        orderEventPublisher.publishOrderCreatedEvent(savedOrder);

        return new OrderResult(savedOrder, false);
    }
}
