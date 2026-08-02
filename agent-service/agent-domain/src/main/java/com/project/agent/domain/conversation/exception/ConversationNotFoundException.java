package com.project.agent.domain.conversation.exception;

import com.project.agent.domain.exception.AggregateNotFoundException;

/**
 * Raised when a conversation cannot be found by its identifier.
 */
public class ConversationNotFoundException extends AggregateNotFoundException {

    public ConversationNotFoundException(String message) {
        super(message);
    }

    public static ConversationNotFoundException of(String id) {
        return new ConversationNotFoundException(
                "Conversation not found: " + id
        );
    }
}
