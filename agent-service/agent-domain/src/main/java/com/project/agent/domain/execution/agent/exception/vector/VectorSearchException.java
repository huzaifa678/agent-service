package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when a similarity search against the vector store fails.
 */
public class VectorSearchException extends DomainException {

    public VectorSearchException(String message) {
        super(message);
    }

    public VectorSearchException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
