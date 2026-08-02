package com.project.agent.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * REST response representing a conversation and its summary metadata.
 */
public record ConversationResponse(
        UUID id,
        UUID tenantId,
        UUID userId,
        String title,
        String status,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {}
