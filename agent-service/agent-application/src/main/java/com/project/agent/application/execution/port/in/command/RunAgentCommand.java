package com.project.agent.application.execution.port.in.command;

import java.util.List;
import java.util.UUID;

/**
 * Command to run one agent turn: append the user message to the conversation,
 * invoke the LLM (with optional tools and RAG context), and record the resulting
 * {@link com.project.agent.domain.execution.agent.AgentExecution}.
 */
public record RunAgentCommand(
        UUID conversationId,
        String userMessage,
        String modelName,
        String providerName,
        List<String> enabledTools
) {}
