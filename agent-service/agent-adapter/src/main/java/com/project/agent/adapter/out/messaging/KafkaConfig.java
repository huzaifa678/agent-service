package com.project.agent.adapter.out.messaging;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer wiring for outbound domain events. Publishes JSON with a String
 * serializer for now.
 *
 * <p>TODO: the platform standard is Avro + Confluent Schema Registry (see
 * billing-service / usage-service). Switch this to {@code KafkaAvroSerializer}
 * with a generated schema from {@code src/main/avro/*.avsc} once the agent usage
 * event contract is finalized.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, String> domainEventProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> domainEventKafkaTemplate(
            ProducerFactory<String, String> domainEventProducerFactory
    ) {
        return new KafkaTemplate<>(domainEventProducerFactory);
    }
}
