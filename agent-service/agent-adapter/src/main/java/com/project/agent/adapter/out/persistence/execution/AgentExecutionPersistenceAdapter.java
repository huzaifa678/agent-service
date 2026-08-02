package com.project.agent.adapter.out.persistence.execution;

import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Implements {@link AgentExecutionRepositoryPort} over Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class AgentExecutionPersistenceAdapter implements AgentExecutionRepositoryPort {

    private final AgentExecutionJpaRepository repository;
    private final AgentExecutionPersistenceMapper mapper;

    @Override
    public AgentExecution save(AgentExecution execution) {
        return mapper.toDomain(repository.save(mapper.toJpa(execution)));
    }

    @Override
    public Optional<AgentExecution> findById(AgentExecutionId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<AgentExecution> findByConversationId(ConversationId conversationId) {
        return repository.findByConversationId(conversationId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
