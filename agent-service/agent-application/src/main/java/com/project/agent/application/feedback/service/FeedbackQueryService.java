package com.project.agent.application.feedback.service;

import com.project.agent.application.feedback.exception.FeedbackNotFoundException;
import com.project.agent.application.feedback.port.in.FeedbackQueries;
import com.project.agent.application.feedback.port.out.FeedbackRepositoryPort;
import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Read side of the feedback context. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedbackQueryService implements FeedbackQueries {

    private final FeedbackRepositoryPort feedbackRepository;

    @Override
    public Feedback getById(UUID feedbackId) {
        FeedbackId id = FeedbackId.of(feedbackId);
        return feedbackRepository.findById(id)
                .orElseThrow(() -> FeedbackNotFoundException.of(id.toString()));
    }

    @Override
    public List<Feedback> byConversation(UUID conversationId) {
        return feedbackRepository.findByConversationId(ConversationId.of(conversationId));
    }
}
