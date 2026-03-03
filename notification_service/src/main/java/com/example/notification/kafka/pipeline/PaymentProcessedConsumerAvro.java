package com.example.notification.kafka.pipeline;

import com.example.avro.PaymentProcessed;
import com.example.notification.kafka.pipeline.dto.NotificationCreatedEvent;
import com.example.notification.observability.KafkaTracePropagator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class PaymentProcessedConsumerAvro {

    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("notification-service");

    private final NotificationEventPublisher notificationEventPublisher;
    private final MeterRegistry meterRegistry;

    public PaymentProcessedConsumerAvro(
            NotificationEventPublisher notificationEventPublisher,
            MeterRegistry meterRegistry) {
        this.notificationEventPublisher = notificationEventPublisher;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "${app.kafka.topics.payments}", groupId = "notification-pipeline-group-v3", containerFactory = "pipelineAvroListenerContainerFactory")
    public void handle(ConsumerRecord<String, PaymentProcessed> record) {
        System.out.println("🔥🔥🔥 KAFKA LISTENER TRIGGERED FOR KEY: " + record.key() + " 🔥🔥🔥");
        PaymentProcessed event = record.value();
        System.out.println("🔥🔥🔥 DESERIALIZED AVRO: " + event + " 🔥🔥🔥");
        Timer.Sample sample = Timer.start(meterRegistry);

        Context extractedContext = KafkaTracePropagator.extractContext(record.headers());
        Span consumeSpan = TRACER.spanBuilder("kafka.consume payments-topic")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(extractedContext)
                .startSpan();

        try (Scope scope = consumeSpan.makeCurrent()) {
            consumeSpan.setAttribute("messaging.system", "kafka");
            consumeSpan.setAttribute("messaging.destination", "payments-topic");
            consumeSpan.setAttribute("messaging.operation", "receive");

            NotificationCreatedEvent notification = NotificationCreatedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .message("Paiement " + event.getStatus() + " pour la commande " + event.getOrderId())
                    .build();
            notificationEventPublisher.publish(notification);
            meterRegistry.counter("notifications_sent_total").increment();
        } finally {
            sample.stop(meterRegistry.timer("notifications.processing.duration"));
            consumeSpan.end();
        }
    }
}
