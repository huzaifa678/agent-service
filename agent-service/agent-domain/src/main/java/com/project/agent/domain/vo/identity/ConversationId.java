package com.project.agent.domain.vo.identity;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a {@link com.project.agent.domain.conversation.Conversation}. */
public record ConversationId(UUID value) {

    public ConversationId {
        Objects.requireNonNull(value, "ConversationId cannot be null");
    }

    public static ConversationId of(UUID value) {
        return new ConversationId(value);
    }

    public static ConversationId of(String value) {
        return new ConversationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
