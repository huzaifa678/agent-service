package com.project.agent.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for renaming a conversation.
 */
public record RenameConversationRequest(
        @NotBlank String title
) {}
