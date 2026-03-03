package com.example.payment.kafka.streams;

import com.example.avro.OrderCreated;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.service.PaymentService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class OrderStreamProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderStreamProcessor.class);
    private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("payment-service");

    private final PaymentService paymentService;
    private final MeterRegistry meterRegistry;

    @Value("${app.kafka.topics.orders}")
    private String ordersTopic;

    public OrderStreamProcessor(PaymentService paymentService, MeterRegistry meterRegistry) {
        this.paymentService = paymentService;
        this.meterRegistry = meterRegistry;
    }

    @Autowired
    public void buildPipeline(StreamsBuilder streamsBuilder) {
        KStream<String, OrderCreated> messageStream = streamsBuilder
                .stream(ordersTopic);

        messageStream.foreach((key, orderCreated) -> {
            LOGGER.info("Kafka Streams processing Order: {}", orderCreated.getOrderId());
            Timer.Sample sample = Timer.start(meterRegistry);

            // Reconstruct Trace from standard Kafka Headers (Kafka Streams hides them in
            // simple API,
            // but we use the new span creation to link it logically or just start a new
            // process span)
            Span consumeSpan = TRACER.spanBuilder("kafka-streams.process orders-topic")
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();

            try (Scope scope = consumeSpan.makeCurrent()) {
                consumeSpan.setAttribute("messaging.system", "kafka-streams");
                consumeSpan.setAttribute("messaging.operation", "process");

                PaymentRequest request = new PaymentRequest();
                request.setOrderId(orderCreated.getOrderId().toString());
                request.setUserId(orderCreated.getUserId().toString());
                request.setAmount(BigDecimal.valueOf(orderCreated.getAmount()));

                // Appel au service transactionnel idempotent
                paymentService.processPayment(request);

                meterRegistry.counter("orders.processed").increment();
                meterRegistry.counter("payments.processed").increment();
            } catch (Exception e) {
                LOGGER.error("Error processing stream for order: {}", orderCreated.getOrderId(), e);
                consumeSpan.recordException(e);
            } finally {
                sample.stop(meterRegistry.timer("orders.processing.duration"));
                consumeSpan.end();
            }
        });
    }
}
