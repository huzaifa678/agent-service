package com.project.agent.domain.vo.conversation;

import java.util.Objects;

/**
 * Value object for the display title of a {@link com.project.agent.domain.conversation.Conversation}.
 * Trimmed on construction; must be non-blank and at most {@value #MAX_LENGTH} characters.
 */
public record ConversationTitle(String value) {

    private static final int MAX_LENGTH = 255;

    public ConversationTitle {
        Objects.requireNonNull(value, "Conversation title cannot be null");

        value = value.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("Conversation title cannot be blank.");
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Conversation title cannot exceed " + MAX_LENGTH + " characters."
            );
        }
    }

    public static ConversationTitle of(String value) {
        return new ConversationTitle(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
