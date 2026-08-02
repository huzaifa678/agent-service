package com.project.agent.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for starting a new conversation.
 */
public record StartConversationRequest(
        @NotNull UUID tenantId,
        @NotNull UUID userId,
        @NotBlank String title
) {}
