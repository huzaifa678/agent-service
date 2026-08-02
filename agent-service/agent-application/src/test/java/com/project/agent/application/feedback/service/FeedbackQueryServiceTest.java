package com.project.agent.application.feedback.service;

import com.project.agent.application.execution.service.common.builder.FeedbackBuilder;
import com.project.agent.application.feedback.exception.FeedbackNotFoundException;
import com.project.agent.application.feedback.port.out.FeedbackRepositoryPort;
import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackQueryServiceTest {

    @Mock
    private FeedbackRepositoryPort feedbackRepository;

    private FeedbackQueryService service;

    @BeforeEach
    void setUp() {

        service = new FeedbackQueryService(
                feedbackRepository
        );
    }

    @Test
    void getById_returnsFeedback() {

        UUID id = UUID.randomUUID();

        Feedback feedback =
                FeedbackBuilder.aFeedback().build();

        when(feedbackRepository.findById(any(FeedbackId.class)))
                .thenReturn(Optional.of(feedback));

        Feedback result =
                service.getById(id);

        assertSame(feedback, result);

        verify(feedbackRepository)
                .findById(any(FeedbackId.class));
    }

    @Test
    void getById_feedbackNotFound_throwsFeedbackNotFoundException() {

        UUID id = UUID.randomUUID();

        when(feedbackRepository.findById(any(FeedbackId.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                FeedbackNotFoundException.class,
                () -> service.getById(id)
        );
    }

    @Test
    void byConversation_returnsFeedbackList() {

        UUID conversationId = UUID.randomUUID();

        List<Feedback> feedback =
                List.of(
                        FeedbackBuilder.aFeedback().build(),
                        FeedbackBuilder.aFeedback().build()
                );

        when(feedbackRepository.findByConversationId(any(ConversationId.class)))
                .thenReturn(feedback);

        List<Feedback> result =
                service.byConversation(conversationId);

        assertEquals(2, result.size());
        assertSame(feedback, result);

        verify(feedbackRepository)
                .findByConversationId(any(ConversationId.class));
    }

    @Test
    void byConversation_returnsEmptyListWhenNoFeedbackExists() {

        UUID conversationId = UUID.randomUUID();

        when(feedbackRepository.findByConversationId(any(ConversationId.class)))
                .thenReturn(List.of());

        List<Feedback> result =
                service.byConversation(conversationId);

        assertEquals(0, result.size());

        verify(feedbackRepository)
                .findByConversationId(any(ConversationId.class));
    }
}
