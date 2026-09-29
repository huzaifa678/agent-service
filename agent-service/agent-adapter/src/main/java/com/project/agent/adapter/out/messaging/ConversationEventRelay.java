package com.project.agent.adapter.out.messaging;

import com.project.agent.adapter.out.persistence.eventstore.ConversationEventEntity;
import com.project.agent.adapter.out.persistence.eventstore.ConversationEventJpaRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Transactional-outbox relay for conversation events. Polls the append-only event store for
 * rows the relay has not yet published, sends each to Kafka (keyed by aggregate id so a
 * conversation's events stay ordered on one partition), and marks them published only after the
 * broker acknowledges.
 *
 * <p>This decouples "the event is durably stored" from "the event is on the bus", which removes
 * the previous persist-then-publish dual write: a crash between the two now just leaves the event
 * unpublished for the next poll. Delivery is therefore at-least-once — consumers dedupe on the
 * {@code eventId} header.
 */
@Component
@Profile("!test")
public class ConversationEventRelay {

    private static final Logger log = LoggerFactory.getLogger(ConversationEventRelay.class);

    private final ConversationEventJpaRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public ConversationEventRelay(
            ConversationEventJpaRepository repository,
            @Qualifier("domainEventKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            @Value("${agent.kafka.topic.conversation-events:agent.conversation.events}") String topic,
            @Value("${agent.conversation.relay.batch-size:100}") int batchSize
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${agent.conversation.relay.fixed-delay-ms:1000}")
    @Transactional
    public void relay() {
        List<ConversationEventEntity> batch =
                repository.findByPublishedFalseOrderByGlobalSeqAsc(PageRequest.of(0, batchSize));
        if (batch.isEmpty()) {
            return;
        }

        List<Long> published = new ArrayList<>(batch.size());
        try {
            for (ConversationEventEntity event : batch) {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        topic, event.getAggregateId().toString(), event.getPayload());
                record.headers().add(new RecordHeader("eventType",
                        event.getEventType().getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("eventId",
                        event.getEventId().toString().getBytes(StandardCharsets.UTF_8)));
                // Block for the broker ack so we only mark rows published once they are on the bus.
                kafkaTemplate.send(record).get();
                published.add(event.getGlobalSeq());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Conversation event relay interrupted after {} of {} events; rest retried next poll",
                    published.size(), batch.size());
        } catch (Exception e) {
            // Stop at the first failure to preserve per-aggregate ordering; retry from here next poll.
            log.error("Conversation event relay failed after {} of {} events; rest retried next poll",
                    published.size(), batch.size(), e);
        }

        if (!published.isEmpty()) {
            repository.markPublished(published, Instant.now());
        }
    }
}
