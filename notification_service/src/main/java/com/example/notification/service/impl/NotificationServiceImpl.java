package com.example.notification.service.impl;

import com.example.notification.event.PaymentEvent;
import com.example.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 🔔 IMPLÉMENTATION DU SERVICE DE NOTIFICATION
 *
 * Dans un environnement de production, ce service utiliserait:
 * - JavaMailSender pour les emails
 * - Twilio/Nexmo pour les SMS
 * - Firebase pour les push notifications
 *
 * Ici, nous simulons l'envoi avec des logs pour la démonstration
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final com.example.notification.repository.NotificationRepository notificationRepository;

    public NotificationServiceImpl(com.example.notification.repository.NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void sendPaymentNotification(PaymentEvent paymentEvent) {
        LOGGER.info("🔔 Traitement de la notification pour le paiement...");

        String userId = paymentEvent.getUserId();
        String orderId = paymentEvent.getOrderId();
        String amount = paymentEvent.getAmount().toString();
        String status = paymentEvent.getStatus();

        // Construire le message
        String subject = "Confirmation de paiement - " + orderId;
        String message = buildNotificationMessage(orderId, amount, status);

        // Envoyer Email
        sendEmail(userId, subject, message);

        // Envoyer SMS
        String smsMessage = String.format(
                "Paiement %s: %s MAD pour la commande %s",
                status, amount, orderId
        );
        sendSms(userId, smsMessage);

        com.example.notification.entity.NotificationEntity notification =
                com.example.notification.entity.NotificationEntity.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .userId(userId)
                        .orderId(orderId)
                        .amount(paymentEvent.getAmount())
                        .status(status)
                        .subject(subject)
                        .emailMessage(message)
                        .smsMessage(smsMessage)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
        notificationRepository.save(notification);

        LOGGER.info("✅ Notifications envoyées avec succès pour orderId: {}", orderId);
    }

    @Override
    public void sendEmail(String userId, String subject, String message) {
        LOGGER.info("========================================");
        LOGGER.info("📧 ENVOI EMAIL");
        LOGGER.info("========================================");
        LOGGER.info("   👤 Destinataire (userId): {}", userId);
        LOGGER.info("   📋 Sujet: {}", subject);
        LOGGER.info("   📝 Message:");
        LOGGER.info("   {}", message);
        LOGGER.info("========================================");
        LOGGER.info("✅ Email envoyé avec succès!");

        // TODO: En production, utiliser JavaMailSender
        // mailSender.send(mailMessage);
    }

    @Override
    public void sendSms(String userId, String message) {
        LOGGER.info("========================================");
        LOGGER.info("📱 ENVOI SMS");
        LOGGER.info("========================================");
        LOGGER.info("   👤 Destinataire (userId): {}", userId);
        LOGGER.info("   📝 Message: {}", message);
        LOGGER.info("========================================");
        LOGGER.info("✅ SMS envoyé avec succès!");

        // TODO: En production, utiliser Twilio
        // twilioClient.sendSms(phoneNumber, message);
    }

    @Override
    public java.util.List<com.example.notification.dto.NotificationMessage> getRecentNotifications(
            int limit,
            String userId,
            String orderId
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, safeLimit);

        if (userId != null && !userId.isBlank() && orderId != null && !orderId.isBlank()) {
            return notificationRepository
                    .findByUserIdAndOrderIdOrderByCreatedAtDesc(userId, orderId, pageable)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        if (userId != null && !userId.isBlank()) {
            return notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        if (orderId != null && !orderId.isBlank()) {
            return notificationRepository
                    .findByOrderIdOrderByCreatedAtDesc(orderId, pageable)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }
        return notificationRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void deleteNotifications(String userId, String orderId) {
        if (userId != null && !userId.isBlank() && orderId != null && !orderId.isBlank()) {
            notificationRepository.deleteByUserIdAndOrderId(userId, orderId);
            return;
        }
        if (userId != null && !userId.isBlank()) {
            notificationRepository.deleteByUserId(userId);
            return;
        }
        if (orderId != null && !orderId.isBlank()) {
            notificationRepository.deleteByOrderId(orderId);
            return;
        }
        notificationRepository.deleteAll();
    }

    private com.example.notification.dto.NotificationMessage toDto(
            com.example.notification.entity.NotificationEntity entity
    ) {
        return com.example.notification.dto.NotificationMessage.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .subject(entity.getSubject())
                .emailMessage(entity.getEmailMessage())
                .smsMessage(entity.getSmsMessage())
                .timestamp(entity.getCreatedAt())
                .build();
    }

    /**
     * Construit le message de notification formaté
     */
    private String buildNotificationMessage(String orderId, String amount, String status) {
        return String.format("""
                ╔══════════════════════════════════════════╗
                ║     🎉 CONFIRMATION DE PAIEMENT 🎉        ║
                ╠══════════════════════════════════════════╣
                ║  Commande: %-28s ║
                ║  Montant:  %-28s ║
                ║  Statut:   %-28s ║
                ╠══════════════════════════════════════════╣
                ║  Votre paiement de %s MAD               
                ║  a été traité avec succès.               ║
                ║                                          ║
                ║  Merci pour votre confiance!             ║
                ╚══════════════════════════════════════════╝
                """, orderId, amount + " MAD", status, amount);
    }
}
