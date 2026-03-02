package com.example.payment.kafka.pipeline;

import com.example.payment.kafka.pipeline.dto.OrderCreatedEvent;
import com.example.payment.kafka.pipeline.dto.PaymentProcessedEvent;
import com.example.payment.observability.KafkaTracePropagator;
import io.micrometer.core.instrument.DistributionSummary;
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
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "json")
public class OrderEventProcessorJson {

    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("payment-service");

    private final PaymentProcessedPublisher paymentProcessedPublisher;
    private final MeterRegistry meterRegistry;
    private final DistributionSummary orderAmountSummary;

    public OrderEventProcessorJson(
            PaymentProcessedPublisher paymentProcessedPublisher,
            MeterRegistry meterRegistry
    ) {
        this.paymentProcessedPublisher = paymentProcessedPublisher;
        this.meterRegistry = meterRegistry;
        this.orderAmountSummary = DistributionSummary.builder("orders.amount")
                .description("Montant des commandes traitees")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.orders}",
            groupId = "order-processor-group",
            containerFactory = "pipelineJsonListenerContainerFactory"
    )
    public void handle(ConsumerRecord<String, OrderCreatedEvent> record) {
        OrderCreatedEvent event = record.value();
        Timer.Sample sample = Timer.start(meterRegistry);

        Context extractedContext = KafkaTracePropagator.extractContext(record.headers());
        Span consumeSpan = TRACER.spanBuilder("kafka.consume orders-topic")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(extractedContext)
                .startSpan();

        try (Scope scope = consumeSpan.makeCurrent()) {
            consumeSpan.setAttribute("messaging.system", "kafka");
            consumeSpan.setAttribute("messaging.destination", "orders-topic");
            consumeSpan.setAttribute("messaging.operation", "receive");

            PaymentProcessedEvent processed = PaymentProcessedEvent.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .status("APPROVED")
                    .build();
            paymentProcessedPublisher.publish(processed);
            meterRegistry.counter("orders.processed").increment();
            meterRegistry.counter("payments.processed").increment();
            orderAmountSummary.record(event.getAmount().doubleValue());
        } finally {
            sample.stop(meterRegistry.timer("orders.processing.duration"));
            consumeSpan.end();
        }
    }
}
