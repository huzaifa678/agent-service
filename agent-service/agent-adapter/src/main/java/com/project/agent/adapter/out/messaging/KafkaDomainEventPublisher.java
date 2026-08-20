package com.project.agent.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent.application.shared.port.out.DomainEventPublisherPort;
import com.project.agent.domain.vo.shared.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Publishes drained aggregate domain events to Kafka. Keyed by event id; the
 * value is the JSON-serialized event. Called by the application services after an
 * aggregate is persisted.
 *
 * <p>See {@link KafkaConfig} for the TODO on moving to Avro + Schema Registry.
 */
@Component
public class KafkaDomainEventPublisher implements DomainEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaDomainEventPublisher(
            @Qualifier("domainEventKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${agent.kafka.topic.usage-recorded:agent.usage.recorded}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishAll(Collection<? extends DomainEvent> events) {
        for (DomainEvent event : events) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(topic, event.eventId().toString(), payload)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                // The send failed after Kafka's own retries; the message never
                                // left the app so there is nothing to dead-letter. Surface it to
                                // Loki via the OTLP appender for alerting/replay.
                                log.error("Failed to publish domain event {} to {}",
                                        event.eventId(), topic, ex);
                            }
                        });
            } catch (JsonProcessingException e) {
                // A serialization failure is a programming error, not a transient one — fail loudly.
                throw new IllegalStateException("Failed to serialize domain event " + event.eventId(), e);
            }
        }
    }
}
