package com.project.agent.adapter.out.persistence.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link FeedbackJpaEntity}. */
public interface FeedbackJpaRepository extends JpaRepository<FeedbackJpaEntity, UUID> {

    List<FeedbackJpaEntity> findByConversationId(UUID conversationId);
}
