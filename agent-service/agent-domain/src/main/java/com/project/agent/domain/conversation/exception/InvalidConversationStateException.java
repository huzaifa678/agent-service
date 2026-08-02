package com.project.agent.domain.conversation.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a conversation is not in a valid state for the requested operation.
 */
public class InvalidConversationStateException
        extends BusinessRuleViolationException {

    public InvalidConversationStateException(String message) {
        super(message);
    }
}
