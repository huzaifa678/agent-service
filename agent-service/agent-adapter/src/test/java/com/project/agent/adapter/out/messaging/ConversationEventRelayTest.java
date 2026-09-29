package com.project.agent.adapter.out.messaging;

import com.project.agent.adapter.out.persistence.eventstore.ConversationEventEntity;
import com.project.agent.adapter.out.persistence.eventstore.ConversationEventJpaRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationEventRelayTest {

    @Mock
    private ConversationEventJpaRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ConversationEventRelay relay() {
        return new ConversationEventRelay(repository, kafkaTemplate, "agent.conversation.events", 100);
    }

    private static ConversationEventEntity event(long globalSeq, UUID aggregateId) {
        return ConversationEventEntity.builder()
                .globalSeq(globalSeq)
                .eventId(UUID.randomUUID())
                .aggregateId(aggregateId)
                .sequence(globalSeq)
                .eventType("conversation.started")
                .payload("{}")
                .occurredAt(Instant.now())
                .published(false)
                .build();
    }

    @Test
    void relay_publishesUnpublishedEventsThenMarksThem() {
        ConversationEventRelay relay = relay();
        UUID aggregateId = UUID.randomUUID();
        ConversationEventEntity e1 = event(1L, aggregateId);
        ConversationEventEntity e2 = event(2L, aggregateId);
        when(repository.findByPublishedFalseOrderByGlobalSeqAsc(any(Pageable.class)))
                .thenReturn(List.of(e1, e2));
        doReturn(CompletableFuture.completedFuture(null)).when(kafkaTemplate).send(any(ProducerRecord.class));

        relay.relay();

        // One send per event, keyed by aggregate id.
        ArgumentCaptor<ProducerRecord<String, String>> record = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate, org.mockito.Mockito.times(2)).send(record.capture());
        assertThat(record.getAllValues()).allSatisfy(r ->
                assertThat(r.key()).isEqualTo(aggregateId.toString()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> marked = ArgumentCaptor.forClass(List.class);
        verify(repository).markPublished(marked.capture(), any(Instant.class));
        assertThat(marked.getValue()).containsExactly(1L, 2L);
    }

    @Test
    void relay_withNothingPending_doesNothing() {
        ConversationEventRelay relay = relay();
        when(repository.findByPublishedFalseOrderByGlobalSeqAsc(any(Pageable.class)))
                .thenReturn(List.of());

        relay.relay();

        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
        verify(repository, never()).markPublished(any(), any());
    }

    @Test
    void relay_marksOnlyEventsPublishedBeforeAFailure() {
        ConversationEventRelay relay = relay();
        UUID aggregateId = UUID.randomUUID();
        ConversationEventEntity e1 = event(1L, aggregateId);
        ConversationEventEntity e2 = event(2L, aggregateId);
        when(repository.findByPublishedFalseOrderByGlobalSeqAsc(any(Pageable.class)))
                .thenReturn(List.of(e1, e2));
        // First send succeeds, second fails: only the first is marked, the rest retried next poll.
        doReturn(CompletableFuture.completedFuture(null))
                .doReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")))
                .when(kafkaTemplate).send(any(ProducerRecord.class));

        relay.relay();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> marked = ArgumentCaptor.forClass(List.class);
        verify(repository).markPublished(marked.capture(), any(Instant.class));
        assertThat(marked.getValue()).containsExactly(1L);
    }
}
