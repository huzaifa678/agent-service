package com.project.agent.application.execution.port.in.query;

import com.project.agent.domain.execution.agent.AgentExecution;

import java.util.List;
import java.util.UUID;

/** Inbound read port for agent executions. */
public interface ExecutionQueries {

    AgentExecution getById(UUID executionId);

    List<AgentExecution> byConversation(UUID conversationId);
}
