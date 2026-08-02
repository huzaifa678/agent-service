package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when the requested LLM provider is temporarily unavailable.
 */
public class ModelUnavailableException extends DomainException {

    public ModelUnavailableException(String provider) {
        super("Model provider is currently unavailable: " + provider);
    }

    public ModelUnavailableException(String provider, Throwable cause) {
        super("Model provider is currently unavailable: " + provider, cause);
    }
}