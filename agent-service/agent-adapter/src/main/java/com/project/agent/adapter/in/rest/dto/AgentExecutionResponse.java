package com.project.agent.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST response for a single agent execution, including token usage, cost, latency
 * and its nested tool executions.
 */
public record AgentExecutionResponse(
        UUID id,
        UUID conversationId,
        String modelName,
        String providerName,
        String status,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        BigDecimal costAmount,
        String costCurrency,
        long latencyMillis,
        Instant startedAt,
        Instant completedAt,
        List<ToolExecutionResponse> toolExecutions
) {}
