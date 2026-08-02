package com.project.agent.adapter.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for submitting feedback on a message (rating 1-5 plus optional comment).
 */
public record SubmitFeedbackRequest(
        @NotNull UUID conversationId,
        @NotNull UUID messageId,
        @Min(1) @Max(5) int rating,
        String comment
) {}
