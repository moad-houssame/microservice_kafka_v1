package com.example.payment.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class KafkaTracePropagator {

    private static final String TRACEPARENT = "traceparent";
    private static final TextMapPropagator PROPAGATOR =
            GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

    private static final TextMapSetter<Headers> HEADER_SETTER = (headers, key, value) -> {
        if (headers == null || value == null) {
            return;
        }
        headers.remove(key);
        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    };

    private static final TextMapGetter<Headers> HEADER_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            List<String> keys = new ArrayList<>();
            if (headers != null) {
                headers.forEach(header -> keys.add(header.key()));
            }
            return keys;
        }

        @Override
        public String get(Headers headers, String key) {
            if (headers == null) {
                return null;
            }
            Header header = headers.lastHeader(key);
            if (header == null) {
                return null;
            }
            return new String(header.value(), StandardCharsets.UTF_8);
        }
    };

    private KafkaTracePropagator() {
    }

    public static String currentTraceParent() {
        Map<String, String> carrier = new HashMap<>();
        PROPAGATOR.inject(Context.current(), carrier, (map, key, value) -> map.put(key, value));
        return carrier.get(TRACEPARENT);
    }

    public static Context extractContext(Headers headers) {
        return PROPAGATOR.extract(Context.current(), headers, HEADER_GETTER);
    }

    public static Context extractContextFromTraceParent(String traceParent) {
        if (traceParent == null || traceParent.isBlank()) {
            return Context.current();
        }
        Map<String, String> carrier = new HashMap<>();
        carrier.put(TRACEPARENT, traceParent);
        TextMapGetter<Map<String, String>> mapGetter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }

            @Override
            public String get(Map<String, String> carrier, String key) {
                if (carrier == null) {
                    return null;
                }
                return carrier.get(key);
            }
        };
        return PROPAGATOR.extract(Context.current(), carrier, mapGetter);
    }

    public static ProducerRecord<String, Object> buildRecordWithCurrentTrace(
            String topic,
            String key,
            Object value
    ) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, value);
        PROPAGATOR.inject(Context.current(), record.headers(), HEADER_SETTER);
        return record;
    }

    public static ProducerRecord<String, Object> buildRecordWithTraceParent(
            String topic,
            String key,
            Object value,
            String traceParent
    ) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, value);
        if (traceParent != null && !traceParent.isBlank()) {
            record.headers().add(TRACEPARENT, traceParent.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }
}
