package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when authentication against an LLM provider fails.
 */
public class ModelAuthenticationException extends DomainException {

    public ModelAuthenticationException(String provider) {
        super("Authentication failed for provider: " + provider);
    }

    public ModelAuthenticationException(String provider, Throwable cause) {
        super("Authentication failed for provider: " + provider, cause);
    }
}
