package com.project.agent.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request body for appending a message to a conversation.
 */
public record AddMessageRequest(
        @NotBlank String role,
        @NotBlank String content,
        @PositiveOrZero int promptTokens,
        @PositiveOrZero int completionTokens
) {}
