package com.project.agent.application.feedback.port.in;

import java.util.UUID;

/** Command to record user feedback (rating 1–5 + optional comment) on a message. */
public record SubmitFeedbackCommand(
        UUID conversationId,
        UUID messageId,
        int rating,
        String comment
) {}
