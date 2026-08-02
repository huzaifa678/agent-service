package com.project.agent.application.feedback.port.out;

import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;

import java.util.List;
import java.util.Optional;

/** Outbound port for persisting and loading {@link Feedback} records. */
public interface FeedbackRepositoryPort {

    Feedback save(Feedback feedback);

    Optional<Feedback> findById(FeedbackId id);

    List<Feedback> findByConversationId(ConversationId conversationId);
}
