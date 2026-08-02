package com.project.agent.domain.feedback;

import com.project.agent.domain.vo.billing.Rating;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import com.project.agent.domain.vo.identity.MessageId;

import java.time.Instant;
import java.util.Objects;

/**
 * Captures user feedback for a specific {@link com.project.agent.domain.message.Message}
 * within a conversation. Holds a 1–5 {@link Rating} and an optional free-text comment.
 * Both fields are mutable after creation to allow corrections; each mutation stamps
 * {@code updatedAt}. Comment is trimmed on construction and update; {@code null} is
 * normalised to an empty string. Equality is by {@link FeedbackId}.
 */
public class Feedback {

    private final FeedbackId id;

    private final ConversationId conversationId;

    private final MessageId messageId;

    private Rating rating;

    private String comment;

    private final Instant createdAt;

    private Instant updatedAt;

    private Feedback(
            FeedbackId id,
            ConversationId conversationId,
            MessageId messageId,
            Rating rating,
            String comment
    ) {
        this.id = Objects.requireNonNull(id);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.messageId = Objects.requireNonNull(messageId);
        this.rating = Objects.requireNonNull(rating);
        this.comment = comment == null ? "" : comment.trim();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Create a new feedback record; {@code comment} may be {@code null} or blank. */
    public static Feedback create(
            FeedbackId id,
            ConversationId conversationId,
            MessageId messageId,
            Rating rating,
            String comment
    ) {
        return new Feedback(
                id,
                conversationId,
                messageId,
                rating,
                comment
        );
    }

    private Feedback(
            FeedbackId id,
            ConversationId conversationId,
            MessageId messageId,
            Rating rating,
            String comment,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.messageId = Objects.requireNonNull(messageId);
        this.rating = Objects.requireNonNull(rating);
        this.comment = comment == null ? "" : comment.trim();
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /** Rehydrate a persisted feedback record with its original timestamps. Persistence-adapter use only. */
    public static Feedback reconstitute(
            FeedbackId id,
            ConversationId conversationId,
            MessageId messageId,
            Rating rating,
            String comment,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Feedback(id, conversationId, messageId, rating, comment, createdAt, updatedAt);
    }

    /** Replace the rating and update {@code updatedAt}. */
    public void updateRating(Rating rating) {
        this.rating = Objects.requireNonNull(rating);
        touch();
    }

    /** Replace the free-text comment (trimmed; {@code null} treated as empty) and update {@code updatedAt}. */
    public void updateComment(String comment) {
        this.comment = comment == null ? "" : comment.trim();
        touch();
    }

    /** Returns {@code true} if a non-blank comment was provided. */
    public boolean hasComment() {
        return !comment.isBlank();
    }

    /** Delegates to {@link Rating#positive()}; returns {@code true} for ratings ≥ 4. */
    public boolean isPositive() {
        return rating.positive();
    }

    public FeedbackId getId() {
        return id;
    }

    public ConversationId getConversationId() {
        return conversationId;
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public Rating getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Stamp {@code updatedAt} with the current wall-clock time. */
    private void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Feedback other)) {
            return false;
        }

        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
