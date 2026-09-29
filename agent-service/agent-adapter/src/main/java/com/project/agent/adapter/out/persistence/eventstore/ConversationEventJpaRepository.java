package com.project.agent.adapter.out.persistence.eventstore;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Spring Data repository over the {@code conversation_event_store} table. */
public interface ConversationEventJpaRepository extends JpaRepository<ConversationEventEntity, Long> {

    /** Events for one aggregate after {@code afterSequence}, in stream order. */
    List<ConversationEventEntity> findByAggregateIdAndSequenceGreaterThanOrderBySequenceAsc(
            UUID aggregateId, long afterSequence);

    /** Unpublished events across all aggregates, in global order — the outbox relay's work queue. */
    List<ConversationEventEntity> findByPublishedFalseOrderByGlobalSeqAsc(Pageable pageable);

    /** Mark a batch of events published in one statement. */
    @Modifying
    @Query("update ConversationEventEntity e set e.published = true, e.publishedAt = :publishedAt "
            + "where e.globalSeq in :globalSeqs")
    int markPublished(@Param("globalSeqs") List<Long> globalSeqs, @Param("publishedAt") Instant publishedAt);
}
