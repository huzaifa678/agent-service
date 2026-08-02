package com.project.agent.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * REST response representing a single message within a conversation.
 */
public record MessageResponse(
        UUID id,
        String role,
        String content,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        Instant createdAt
) {}
