package com.example.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 🌐 Controller de santé pour le Notification Service
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final com.example.notification.service.NotificationService notificationService;

    public NotificationController(com.example.notification.service.NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<com.example.notification.dto.NotificationMessage>> getRecent(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "orderId", required = false) String orderId
    ) {
        return ResponseEntity.ok(notificationService.getRecentNotifications(limit, userId, orderId));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(name = "orderId", required = false) String orderId
    ) {
        notificationService.deleteNotifications(userId, orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "status", "UP",
                "message", "🔔 Notification Service is running and listening to Kafka!"
        ));
    }
}
