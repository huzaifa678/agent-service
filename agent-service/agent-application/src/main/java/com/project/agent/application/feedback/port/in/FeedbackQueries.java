package com.project.agent.application.feedback.port.in;

import com.project.agent.domain.feedback.Feedback;

import java.util.List;
import java.util.UUID;

/** Inbound read port for feedback. */
public interface FeedbackQueries {

    Feedback getById(UUID feedbackId);

    List<Feedback> byConversation(UUID conversationId);
}
