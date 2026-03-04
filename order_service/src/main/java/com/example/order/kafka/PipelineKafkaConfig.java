package com.example.order.kafka;

import com.example.avro.OrderCreated;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@ConditionalOnProperty(name = "app.kafka.message-format", havingValue = "avro")
public class PipelineKafkaConfig {

    @Bean("pipelineAvroKafkaTemplate")
    public KafkaTemplate<String, OrderCreated> pipelineAvroKafkaTemplate(
            ProducerFactory<String, OrderCreated> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
