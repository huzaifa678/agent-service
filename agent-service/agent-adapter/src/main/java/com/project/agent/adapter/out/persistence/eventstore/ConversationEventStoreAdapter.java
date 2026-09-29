package com.project.agent.adapter.out.persistence.eventstore;

import com.project.agent.application.conversation.port.out.ConversationEventStorePort;
import com.project.agent.domain.conversation.event.ConversationEvent;
import com.project.agent.domain.vo.identity.ConversationId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA-backed {@link ConversationEventStorePort}. Appends are asserted against
 * {@code expectedVersion}: the events are numbered {@code expectedVersion + 1 ..} and the
 * unique {@code (aggregate_id, sequence)} constraint rejects a concurrent writer's clashing
 * insert, which we surface as an {@link OptimisticLockingFailureException}.
 */
@Component
@RequiredArgsConstructor
public class ConversationEventStoreAdapter implements ConversationEventStorePort {

    private final ConversationEventJpaRepository repository;
    private final ConversationEventSerializer serializer;

    @Override
    public void append(ConversationId aggregateId, long expectedVersion, List<ConversationEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        long expected = expectedVersion;
        List<ConversationEventEntity> rows = new ArrayList<>(events.size());
        for (ConversationEvent event : events) {
            expected++;
            if (event.sequence() != expected) {
                // Defensive: the aggregate must produce a gap-free run starting at expectedVersion+1.
                throw new IllegalStateException("Non-contiguous event sequence for conversation "
                        + aggregateId + ": expected " + expected + " but event carried " + event.sequence());
            }
            rows.add(ConversationEventEntity.builder()
                    .eventId(event.eventId())
                    .aggregateId(aggregateId.value())
                    .tenantId(tenantIdOf(event))
                    .sequence(event.sequence())
                    .eventType(serializer.typeOf(event))
                    .payload(serializer.serialize(event))
                    .occurredAt(event.occurredAt())
                    .published(false)
                    .build());
        }

        try {
            repository.saveAll(rows);
            // Force the insert (and thus the unique-constraint check) to happen now, inside the
            // caller's transaction, so a concurrency clash is reported as a save failure.
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new OptimisticLockingFailureException(
                    "Concurrent modification of conversation " + aggregateId
                            + " (expected version " + expectedVersion + ")", e);
        }
    }

    @Override
    public List<ConversationEvent> loadAfter(ConversationId aggregateId, long afterSequence) {
        return repository
                .findByAggregateIdAndSequenceGreaterThanOrderBySequenceAsc(aggregateId.value(), afterSequence)
                .stream()
                .map(row -> serializer.deserialize(row.getEventType(), row.getPayload()))
                .toList();
    }

    private static java.util.UUID tenantIdOf(ConversationEvent event) {
        return event instanceof ConversationEvent.Started started ? started.tenantId() : null;
    }
}
