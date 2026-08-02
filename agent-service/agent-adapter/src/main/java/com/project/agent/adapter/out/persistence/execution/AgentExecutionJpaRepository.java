package com.project.agent.adapter.out.persistence.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data repository for {@link AgentExecutionJpaEntity}. */
public interface AgentExecutionJpaRepository extends JpaRepository<AgentExecutionJpaEntity, UUID> {

    List<AgentExecutionJpaEntity> findByConversationId(UUID conversationId);
}
