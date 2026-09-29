package com.project.agent.adapter.out.persistence.eventstore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * A materialised conversation state at a known {@code version}. One row per aggregate (upserted):
 * loading it and replaying only the later events bounds replay cost. Snapshots are derived data
 * and can be dropped and rebuilt from the event store at any time.
 */
@Entity
@Table(name = "conversation_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSnapshotEntity {

    @Id
    @Column(name = "aggregate_id")
    private UUID aggregateId;

    @Column(name = "version", nullable = false)
    private long version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state", nullable = false, columnDefinition = "jsonb")
    private String state;

    @Column(name = "taken_at", nullable = false)
    private Instant takenAt;
}
