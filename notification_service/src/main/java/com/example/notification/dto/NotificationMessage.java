package com.example.notification.dto;

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
public class NotificationMessage {

    private String id;
    private String userId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String subject;
    private String emailMessage;
    private String smsMessage;
    private LocalDateTime timestamp;
}
