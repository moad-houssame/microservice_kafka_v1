package com.example.notification.service;

import com.example.notification.event.PaymentEvent;

/**
 * 🔔 Service de notification
 */
public interface NotificationService {

    /**
     * Envoie une notification de paiement (Email + SMS)
     */
    void sendPaymentNotification(PaymentEvent paymentEvent);

    /**
     * Envoie un email
     */
    void sendEmail(String userId, String subject, String message);

    /**
     * Envoie un SMS
     */
    void sendSms(String userId, String message);

    /**
     * Récupère les notifications récentes (ordre du plus récent au plus ancien)
     */
    java.util.List<com.example.notification.dto.NotificationMessage> getRecentNotifications(
            int limit,
            String userId,
            String orderId
    );

    /**
     * Supprime les notifications (toutes ou filtrées)
     */
    void deleteNotifications(String userId, String orderId);
}
