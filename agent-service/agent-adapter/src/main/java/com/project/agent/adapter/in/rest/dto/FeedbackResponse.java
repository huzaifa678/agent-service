package com.project.agent.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * REST response representing user feedback on a message.
 */
public record FeedbackResponse(
        UUID id,
        UUID conversationId,
        UUID messageId,
        int rating,
        boolean positive,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {}
