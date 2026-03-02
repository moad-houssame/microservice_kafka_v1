package com.example.payment.kafka.pipeline;

import com.example.avro.OrderCreated;
import com.example.payment.kafka.pipeline.dto.PaymentProcessedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class OrderEventProcessorAvro {

    private final PaymentProcessedPublisher paymentProcessedPublisher;
    private final MeterRegistry meterRegistry;
    private static final io.opentelemetry.api.trace.Tracer TRACER =
            io.opentelemetry.api.GlobalOpenTelemetry.getTracer("payment-service");

    public OrderEventProcessorAvro(
            PaymentProcessedPublisher paymentProcessedPublisher,
            MeterRegistry meterRegistry
    ) {
        this.paymentProcessedPublisher = paymentProcessedPublisher;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders}",
            groupId = "order-processor-group",
            containerFactory = "pipelineAvroListenerContainerFactory"
    )
    public void handle(org.apache.kafka.clients.consumer.ConsumerRecord<String, OrderCreated> record) {
        OrderCreated event = record.value();
        Timer.Sample sample = Timer.start(meterRegistry);

        io.opentelemetry.context.Context extractedContext =
                com.example.payment.observability.KafkaTracePropagator.extractContext(record.headers());
        io.opentelemetry.api.trace.Span consumeSpan = TRACER.spanBuilder("kafka.consume orders-topic")
                .setSpanKind(io.opentelemetry.api.trace.SpanKind.CONSUMER)
                .setParent(extractedContext)
                .startSpan();

        try (io.opentelemetry.context.Scope scope = consumeSpan.makeCurrent()) {
            consumeSpan.setAttribute("messaging.system", "kafka");
            consumeSpan.setAttribute("messaging.destination", "orders-topic");
            consumeSpan.setAttribute("messaging.operation", "receive");

            PaymentProcessedEvent processed = PaymentProcessedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(java.math.BigDecimal.valueOf(event.getAmount()))
                    .status("APPROVED")
                    .build();
            paymentProcessedPublisher.publish(processed);
            meterRegistry.counter("orders.processed").increment();
            meterRegistry.counter("payments.processed").increment();
        } finally {
            sample.stop(meterRegistry.timer("orders.processing.duration"));
            consumeSpan.end();
        }
    }
}