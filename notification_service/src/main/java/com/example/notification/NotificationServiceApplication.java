package com.example.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 🔔 NOTIFICATION SERVICE
 *
 * Microservice qui consomme les événements Kafka du Payment Service
 * et envoie des notifications (Email/SMS) aux utilisateurs
 *
 * Architecture:
 * Payment Service → Kafka (payment-topic) → Notification Service → Email/SMS
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
