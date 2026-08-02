package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a requested embedding model is not supported.
 */
public class UnsupportedEmbeddingModelException extends BusinessRuleViolationException {

    public UnsupportedEmbeddingModelException(String model) {
        super("Unsupported embedding model: " + model);
    }
}
