package com.project.agent.application.feedback.service;

import com.project.agent.application.feedback.exception.FeedbackNotFoundException;
import com.project.agent.application.feedback.port.in.SubmitFeedbackCommand;
import com.project.agent.application.feedback.port.in.SubmitFeedbackUseCase;
import com.project.agent.application.feedback.port.in.UpdateFeedbackCommand;
import com.project.agent.application.feedback.port.in.UpdateFeedbackUseCase;
import com.project.agent.application.feedback.port.out.FeedbackRepositoryPort;
import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.billing.Rating;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import com.project.agent.domain.vo.identity.MessageId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Write side of the feedback context. */
@Service
@Transactional
@RequiredArgsConstructor
public class FeedbackCommandService implements SubmitFeedbackUseCase, UpdateFeedbackUseCase {

    private final FeedbackRepositoryPort feedbackRepository;

    @Override
    public Feedback submit(SubmitFeedbackCommand command) {
        Feedback feedback = Feedback.create(
                FeedbackId.of(UUID.randomUUID()),
                ConversationId.of(command.conversationId()),
                MessageId.of(command.messageId()),
                Rating.of(command.rating()),
                command.comment()
        );
        return feedbackRepository.save(feedback);
    }

    @Override
    public Feedback update(UpdateFeedbackCommand command) {
        FeedbackId id = FeedbackId.of(command.feedbackId());
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> FeedbackNotFoundException.of(id.toString()));

        if (command.rating() != null) {
            feedback.updateRating(Rating.of(command.rating()));
        }
        if (command.comment() != null) {
            feedback.updateComment(command.comment());
        }
        return feedbackRepository.save(feedback);
    }
}
