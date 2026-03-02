package com.example.notification.observability;

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
import java.util.List;

public final class KafkaTracePropagator {

    private static final TextMapPropagator PROPAGATOR = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

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

    public static Context extractContext(Headers headers) {
        return PROPAGATOR.extract(Context.current(), headers, HEADER_GETTER);
    }

    public static ProducerRecord<String, Object> buildRecordWithCurrentTrace(
            String topic,
            String key,
            Object value) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, value);
        PROPAGATOR.inject(Context.current(), record.headers(), HEADER_SETTER);
        return record;
    }
}
