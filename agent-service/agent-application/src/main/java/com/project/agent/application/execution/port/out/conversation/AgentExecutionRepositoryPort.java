package com.project.agent.application.execution.port.out.conversation;

import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;

import java.util.List;
import java.util.Optional;

/** Outbound port for persisting and loading {@link AgentExecution} records (with their tool executions). */
public interface AgentExecutionRepositoryPort {

    AgentExecution save(AgentExecution execution);

    Optional<AgentExecution> findById(AgentExecutionId id);

    List<AgentExecution> findByConversationId(ConversationId conversationId);
}
