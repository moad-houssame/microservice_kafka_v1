package com.example.notification.kafka.pipeline;

import com.example.notification.kafka.pipeline.dto.NotificationCreatedEvent;
import com.example.notification.observability.KafkaTracePropagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "json")
public class JsonNotificationEventPublisher implements NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String notificationsTopic;

    public JsonNotificationEventPublisher(
            @Qualifier("pipelineJsonKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.notifications}") String notificationsTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationsTopic = notificationsTopic;
    }

    @Override
    public void publish(NotificationCreatedEvent event) {
        kafkaTemplate.send(
                KafkaTracePropagator.buildRecordWithCurrentTrace(
                        notificationsTopic,
                        event.getOrderId(),
                        event
                )
        );
    }
}
