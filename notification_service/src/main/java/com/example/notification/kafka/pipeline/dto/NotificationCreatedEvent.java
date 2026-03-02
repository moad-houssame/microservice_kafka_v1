package com.example.notification.kafka.pipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreatedEvent {

    private String orderId;
    private String userId;
    private String message;
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();
}
