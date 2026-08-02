package com.project.agent.adapter.out.persistence.execution;

import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;

import java.util.List;
import java.util.Optional;

/**
 * Stub {@link com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort}
 * implementation; agent-execution persistence is not yet wired, so every operation is a no-op.
 */
public class AgentExecutionRepositoryAdapter implements AgentExecutionRepositoryPort {

    @Override
    public AgentExecution save(AgentExecution execution) {
        return null;
    }

    @Override
    public Optional<AgentExecution> findById(AgentExecutionId id) {
        return Optional.empty();
    }

    @Override
    public List<AgentExecution> findByConversationId(ConversationId conversationId) {
        return List.of();
    }
}
