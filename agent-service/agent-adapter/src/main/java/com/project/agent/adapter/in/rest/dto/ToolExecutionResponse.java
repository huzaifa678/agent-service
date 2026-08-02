package com.project.agent.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * REST response representing a single tool execution within an agent execution.
 */
public record ToolExecutionResponse(
        UUID id,
        String toolName,
        String request,
        String response,
        String status,
        long latencyMillis,
        Instant startedAt,
        Instant completedAt
) {}
