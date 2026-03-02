package com.example.notification.kafka.pipeline;

import com.example.notification.kafka.pipeline.dto.NotificationCreatedEvent;

public interface NotificationEventPublisher {
    void publish(NotificationCreatedEvent event);
}
