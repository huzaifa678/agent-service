package com.project.agent.application.feedback.port.in;

import java.util.UUID;

/**
 * Command to amend existing feedback. A {@code null} field leaves that attribute
 * unchanged, so a caller can update only the rating or only the comment.
 */
public record UpdateFeedbackCommand(
        UUID feedbackId,
        Integer rating,
        String comment
) {}
