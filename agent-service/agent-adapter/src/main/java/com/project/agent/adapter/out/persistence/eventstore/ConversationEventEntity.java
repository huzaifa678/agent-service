package com.project.agent.adapter.out.persistence.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A single row in the append-only conversation event store. The pair
 * {@code (aggregate_id, sequence)} is unique — that constraint is what enforces optimistic
 * concurrency on append. {@code global_seq} gives a stream-wide total order the relay walks to
 * publish events to Kafka exactly once.
 */
@Entity
@Table(
        name = "conversation_event_store",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_conversation_event_aggregate_sequence",
                        columnNames = {"aggregate_id", "sequence"}),
                @UniqueConstraint(name = "uq_conversation_event_event_id",
                        columnNames = {"event_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationEventEntity {

    /** Stream-wide monotonic ordering key (identity); drives the outbox relay. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "global_seq")
    private Long globalSeq;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    /** Aggregate-local version this event advances the stream to (1-based). */
    @Column(name = "sequence", nullable = false)
    private long sequence;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Outbox flag: false until the relay has published this event to Kafka. */
    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;
}
