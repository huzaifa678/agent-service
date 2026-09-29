package com.project.agent.adapter.out.persistence.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data repository over the {@code conversation_snapshot} table. */
public interface ConversationSnapshotJpaRepository extends JpaRepository<ConversationSnapshotEntity, UUID> {
}
