package com.project.agent.domain.vo.identity;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a piece of user feedback submitted for an agent response. */
public record FeedbackId(UUID value) {

    public FeedbackId {
        Objects.requireNonNull(value, "feedback id must not be null");
    }

    public static FeedbackId of(UUID value) {
        return new FeedbackId(value);
    }

    public static FeedbackId of(String value) {
        return new FeedbackId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
