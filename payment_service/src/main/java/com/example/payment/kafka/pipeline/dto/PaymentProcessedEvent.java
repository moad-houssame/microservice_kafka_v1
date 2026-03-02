package com.example.payment.kafka.pipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String status;
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
