package com.project.agent.application.execution.service.common.builder;

import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.billing.Rating;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import com.project.agent.domain.vo.identity.MessageId;

import java.util.UUID;

public class FeedbackBuilder {

    private FeedbackId feedbackId =
            FeedbackId.of(UUID.randomUUID());

    private ConversationId conversationId =
            ConversationId.of(UUID.randomUUID());

    private MessageId messageId =
            MessageId.of(UUID.randomUUID());

    private Rating rating =
            Rating.of(5);

    private String comment =
            "Great response";

    private FeedbackBuilder() {
    }

    public static FeedbackBuilder aFeedback() {
        return new FeedbackBuilder();
    }

    public FeedbackBuilder withFeedbackId(
            FeedbackId feedbackId
    ) {
        this.feedbackId = feedbackId;
        return this;
    }

    public FeedbackBuilder withConversationId(
            ConversationId conversationId
    ) {
        this.conversationId = conversationId;
        return this;
    }

    public FeedbackBuilder withMessageId(
            MessageId messageId
    ) {
        this.messageId = messageId;
        return this;
    }

    public FeedbackBuilder withRating(
            int rating
    ) {
        this.rating = Rating.of(rating);
        return this;
    }

    public FeedbackBuilder withComment(
            String comment
    ) {
        this.comment = comment;
        return this;
    }

    public Feedback build() {
        return Feedback.create(
                feedbackId,
                conversationId,
                messageId,
                rating,
                comment
        );
    }
}
