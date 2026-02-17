package com.example.notification.repository;

import com.example.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    Page<NotificationEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<NotificationEntity> findByOrderIdOrderByCreatedAtDesc(String orderId, Pageable pageable);

    Page<NotificationEntity> findByUserIdAndOrderIdOrderByCreatedAtDesc(
            String userId,
            String orderId,
            Pageable pageable
    );

    long deleteByUserId(String userId);

    long deleteByOrderId(String orderId);

    long deleteByUserIdAndOrderId(String userId, String orderId);
}
