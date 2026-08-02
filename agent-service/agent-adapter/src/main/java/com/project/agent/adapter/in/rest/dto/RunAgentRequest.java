package com.project.agent.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request body for running the agent against a conversation turn.
 */
public record RunAgentRequest(
        @NotNull UUID conversationId,
        @NotBlank String userMessage,
        @NotBlank String modelName,
        @NotBlank String providerName,
        List<String> enabledTools
) {}
