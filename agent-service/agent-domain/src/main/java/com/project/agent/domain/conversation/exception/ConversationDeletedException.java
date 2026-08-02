package com.project.agent.domain.conversation.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when an operation targets a conversation that has been (soft-)deleted.
 */
public class ConversationDeletedException
        extends BusinessRuleViolationException {

    public ConversationDeletedException() {
        super("Conversation has been deleted.");
    }
}
