package com.example.notification.kafka.pipeline;

import com.example.avro.NotificationCreated;
import com.example.notification.kafka.pipeline.dto.NotificationCreatedEvent;
import com.example.notification.observability.KafkaTracePropagator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class AvroNotificationEventPublisher implements NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String notificationsTopic;

    public AvroNotificationEventPublisher(
            @Qualifier("pipelineAvroKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.notifications}") String notificationsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationsTopic = notificationsTopic;
    }

    @Override
    public void publish(NotificationCreatedEvent event) {
        NotificationCreated avroEvent = NotificationCreated.newBuilder()
                .setOrderId(event.getOrderId())
                .setUserId(event.getUserId())
                .setMessage(event.getMessage())
                .setSentAt(Instant.now().toString())
                .build();

        kafkaTemplate.send(
                KafkaTracePropagator.buildRecordWithCurrentTrace(
                        notificationsTopic,
                        event.getOrderId(),
                        avroEvent));
    }
}
