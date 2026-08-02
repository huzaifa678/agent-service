package com.project.agent.domain.conversation.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when an operation targets a conversation that has already been archived.
 */
public class ConversationAlreadyArchivedException
        extends BusinessRuleViolationException {

    public ConversationAlreadyArchivedException() {
        super("Conversation is already archived.");
    }
}
