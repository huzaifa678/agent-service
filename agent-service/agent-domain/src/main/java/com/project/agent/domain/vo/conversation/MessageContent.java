package com.project.agent.domain.vo.conversation;

import java.util.Objects;

/**
 * Value object for the text body of a {@link com.project.agent.domain.message.Message}
 * or a tool call request/response payload. Must be non-blank and at most
 * {@value #MAX_LENGTH} characters to prevent unbounded storage growth.
 */
public record MessageContent(String value) {

    private static final int MAX_LENGTH = 100_000;

    public MessageContent {

        Objects.requireNonNull(value, "Message content cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank.");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Message content exceeds maximum length."
            );
        }
    }

    /** Returns the character length of the content. */
    public int length() {
        return value.length();
    }

    public static MessageContent of(String value) {
        return new MessageContent(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
