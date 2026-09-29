package com.project.agent.application.conversation.port.out;

import com.project.agent.domain.conversation.event.ConversationEvent;
import com.project.agent.domain.vo.identity.ConversationId;

import java.util.List;

/**
 * Outbound port for the append-only conversation event store — the write-side source of
 * truth. Implemented in agent-adapter over the {@code conversation_event_store} table.
 */
public interface ConversationEventStorePort {

    /**
     * Append {@code events} for an aggregate, asserting that the stream is currently at
     * {@code expectedVersion} (0 for a new stream). Implementations enforce this atomically
     * via the unique {@code (aggregate_id, sequence)} constraint and must translate a
     * clash into an optimistic-locking failure so the command can be retried.
     *
     * @throws org.springframework.dao.OptimisticLockingFailureException if another writer
     *         advanced the stream past {@code expectedVersion}
     */
    void append(ConversationId aggregateId, long expectedVersion, List<ConversationEvent> events);

    /** Load the events for an aggregate whose sequence is strictly greater than {@code afterSequence}, in order. */
    List<ConversationEvent> loadAfter(ConversationId aggregateId, long afterSequence);
}
