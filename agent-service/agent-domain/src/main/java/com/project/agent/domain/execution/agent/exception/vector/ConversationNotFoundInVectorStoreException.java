package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when no vectors exist for a conversation in the vector store.
 */
public class ConversationNotFoundInVectorStoreException extends DomainException {

    public ConversationNotFoundInVectorStoreException(String conversationId) {
        super("Conversation not found in vector store: " + conversationId);
    }
}
