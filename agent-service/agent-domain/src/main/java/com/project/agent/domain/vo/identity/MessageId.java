package com.project.agent.domain.vo.identity;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a {@link com.project.agent.domain.message.Message}. */
public record MessageId(UUID value) {

    public MessageId {
        Objects.requireNonNull(value, "message id must not be null");
    }

    public static MessageId of(UUID value) {
        return new MessageId(value);
    }

    public static MessageId of(String value) {
        return new MessageId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
