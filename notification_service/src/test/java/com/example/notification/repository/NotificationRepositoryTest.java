package com.example.notification.repository;

import com.example.notification.entity.NotificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void shouldSaveAndFindByUserId() {
        NotificationEntity entity = NotificationEntity.builder()
                .id("test-1")
                .userId("user-1")
                .orderId("order-1")
                .amount(new BigDecimal("10.00"))
                .status("SUCCESS")
                .subject("Test subject")
                .emailMessage("Email")
                .smsMessage("SMS")
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(entity);

        var result = notificationRepository
                .findByUserIdOrderByCreatedAtDesc("user-1", PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo("order-1");
    }
}
