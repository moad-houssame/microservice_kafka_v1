package com.example.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * ⚙️ Configuration Kafka pour Exactly Once Processing
 *
 * Points clés:
 * 1. Idempotent Producer (enable.idempotence=true)
 * 2. Transaction support (transaction-id-prefix)
 * 3. Acknowledgement de tous les replicas (acks=all)
 */
@Configuration
public class KafkaConfig {

    /**
     * 🎯 Création du topic "payment-topic"
     * - 3 partitions pour scalabilité
     * - 1 replica (en dev, augmenter en prod)
     */
    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name("payment-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("orders-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name("payments-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("notifications-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

}