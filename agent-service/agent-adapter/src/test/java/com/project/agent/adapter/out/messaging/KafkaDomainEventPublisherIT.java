package com.project.agent.adapter.out.messaging;

import com.project.agent.adapter.support.KafkaContainerConfig;
import com.project.agent.application.shared.port.out.DomainEventPublisherPort;
import com.project.agent.domain.vo.shared.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringJUnitConfig
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class KafkaDomainEventPublisherIT extends KafkaContainerConfig {

    @Autowired
    private DomainEventPublisherPort publisher;

    private TestDomainEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent(UUID.randomUUID(), Instant.now());
    }

    @Test
    void publishAll_sendsEventToKafkaTopic() {
        publisher.publishAll(List.of(testEvent));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String payload = consumeFromTopic(testEvent.eventId().toString());
            assertThat(payload).isNotNull();
            assertThat(payload).contains(testEvent.eventId().toString());
        });
    }

    @Test
    void publishAll_multipleEvents_publishesAll() {
        TestDomainEvent event1 = new TestDomainEvent(UUID.randomUUID(), Instant.now());
        TestDomainEvent event2 = new TestDomainEvent(UUID.randomUUID(), Instant.now());

        publisher.publishAll(List.of(event1, event2));

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            String payload1 = consumeFromTopic(event1.eventId().toString());
            String payload2 = consumeFromTopic(event2.eventId().toString());
            assertThat(payload1).isNotNull();
            assertThat(payload2).isNotNull();
        });
    }

    @Test
    void publishAll_emptyCollection_doesNotThrow() {
        publisher.publishAll(List.of());
        assertThat(true).isTrue();
    }

    private String consumeFromTopic(String expectedKey) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("agent.usage.recorded"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

            for (var record : records) {
                if (expectedKey.equals(record.key())) {
                    return record.value();
                }
            }
        }
        return null;
    }

    private record TestDomainEvent(UUID eventId, Instant occurredAt) implements DomainEvent {
    }
}
