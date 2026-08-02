package com.project.agent.adapter.out.persistence.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link ConversationJpaEntity}. */
public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    List<ConversationJpaEntity> findByUserId(UUID userId);
}
