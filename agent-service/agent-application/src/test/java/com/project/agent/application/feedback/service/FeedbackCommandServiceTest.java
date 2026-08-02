package com.project.agent.application.feedback.service;

import com.project.agent.application.execution.service.common.builder.FeedbackBuilder;
import com.project.agent.application.feedback.exception.FeedbackNotFoundException;
import com.project.agent.application.feedback.port.in.SubmitFeedbackCommand;
import com.project.agent.application.feedback.port.in.UpdateFeedbackCommand;
import com.project.agent.application.feedback.port.out.FeedbackRepositoryPort;
import com.project.agent.domain.feedback.Feedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackCommandServiceTest {

    @Mock
    private FeedbackRepositoryPort feedbackRepository;

    private FeedbackCommandService service;

    @BeforeEach
    void setUp() {

        service = new FeedbackCommandService(
                feedbackRepository
        );
    }

    @Test
    void submit_savesFeedback() {

        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Feedback feedback =
                FeedbackBuilder.aFeedback().build();

        SubmitFeedbackCommand command =
                new SubmitFeedbackCommand(
                        conversationId,
                        messageId,
                        5,
                        "Excellent response"
                );

        when(feedbackRepository.save(any()))
                .thenReturn(feedback);

        Feedback saved =
                service.submit(command);

        assertSame(feedback, saved);

        verify(feedbackRepository)
                .save(any(Feedback.class));
    }

    @Test
    void update_updatesRatingAndComment() {

        UUID feedbackId = UUID.randomUUID();

        Feedback feedback =
                spy(FeedbackBuilder.aFeedback().build());

        UpdateFeedbackCommand command =
                new UpdateFeedbackCommand(
                        feedbackId,
                        3,
                        "Needs improvement"
                );

        when(feedbackRepository.findById(any()))
                .thenReturn(Optional.of(feedback));

        when(feedbackRepository.save(any()))
                .thenReturn(feedback);

        Feedback updated =
                service.update(command);

        assertSame(feedback, updated);

        verify(feedback)
                .updateRating(any());

        verify(feedback)
                .updateComment("Needs improvement");

        verify(feedbackRepository)
                .save(feedback);
    }

    @Test
    void update_onlyRating_updatesRating() {

        UUID feedbackId = UUID.randomUUID();

        Feedback feedback =
                spy(FeedbackBuilder.aFeedback().build());

        UpdateFeedbackCommand command =
                new UpdateFeedbackCommand(
                        feedbackId,
                        4,
                        null
                );

        when(feedbackRepository.findById(any()))
                .thenReturn(Optional.of(feedback));

        when(feedbackRepository.save(any()))
                .thenReturn(feedback);

        service.update(command);

        verify(feedback)
                .updateRating(any());

        verify(feedback, never())
                .updateComment(anyString());
    }

    @Test
    void update_onlyComment_updatesComment() {

        UUID feedbackId = UUID.randomUUID();

        Feedback feedback =
                spy(FeedbackBuilder.aFeedback().build());

        UpdateFeedbackCommand command =
                new UpdateFeedbackCommand(
                        feedbackId,
                        null,
                        "Updated comment"
                );

        when(feedbackRepository.findById(any()))
                .thenReturn(Optional.of(feedback));

        when(feedbackRepository.save(any()))
                .thenReturn(feedback);

        service.update(command);

        verify(feedback, never())
                .updateRating(any());

        verify(feedback)
                .updateComment("Updated comment");
    }

    @Test
    void update_feedbackNotFound_throwsFeedbackNotFoundException() {

        UUID feedbackId = UUID.randomUUID();

        UpdateFeedbackCommand command =
                new UpdateFeedbackCommand(
                        feedbackId,
                        5,
                        "Great"
                );

        when(feedbackRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(
                FeedbackNotFoundException.class,
                () -> service.update(command)
        );

        verify(feedbackRepository, never())
                .save(any());
    }

    @Test
    void update_noChanges_stillSavesFeedback() {

        UUID feedbackId = UUID.randomUUID();

        Feedback feedback =
                spy(FeedbackBuilder.aFeedback().build());

        UpdateFeedbackCommand command =
                new UpdateFeedbackCommand(
                        feedbackId,
                        null,
                        null
                );

        when(feedbackRepository.findById(any()))
                .thenReturn(Optional.of(feedback));

        when(feedbackRepository.save(any()))
                .thenReturn(feedback);

        Feedback updated =
                service.update(command);

        assertSame(feedback, updated);

        verify(feedback, never())
                .updateRating(any());

        verify(feedback, never())
                .updateComment(anyString());

        verify(feedbackRepository)
                .save(feedback);
    }
}
