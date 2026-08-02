package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised for a generic, unclassified error returned by an LLM provider.
 */
public class ModelProviderException extends DomainException {

    public ModelProviderException(String message) {
        super(message);
    }

    public ModelProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
