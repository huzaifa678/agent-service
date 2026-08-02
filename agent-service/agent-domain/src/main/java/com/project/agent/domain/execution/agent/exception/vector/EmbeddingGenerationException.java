package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when the embedding model fails to generate an embedding.
 */
public class EmbeddingGenerationException extends DomainException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
