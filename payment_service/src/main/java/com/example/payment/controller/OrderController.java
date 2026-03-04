package com.example.payment.controller;

import com.example.payment.kafka.pipeline.OrderEventPublisher;
import com.example.payment.kafka.pipeline.dto.OrderCreatedEvent;
import io.opentelemetry.api.trace.Span;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderEventPublisher orderEventPublisher;
    private final Counter ordersProcessedCounter;

    public OrderController(OrderEventPublisher orderEventPublisher, MeterRegistry meterRegistry) {
        this.orderEventPublisher = orderEventPublisher;
        this.ordersProcessedCounter = Counter.builder("orders_processed_total")
                .description("Total number of orders processed")
                .register(meterRegistry);
    }

    @PostMapping
    public ResponseEntity<Void> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(request.orderId)
                .userId(request.userId)
                .amount(request.amount)
                .build();
        orderEventPublisher.publish(event);
        ordersProcessedCounter.increment();
        return ResponseEntity.accepted()
                .header("X-Trace-Id", Span.current().getSpanContext().getTraceId())
                .header("X-Span-Id", Span.current().getSpanContext().getSpanId())
                .build();
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
