package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when an LLM provider rejects a request because a rate limit was exceeded.
 */
public class ModelRateLimitException extends DomainException {

    public ModelRateLimitException(String provider) {
        super("Rate limit exceeded for provider: " + provider);
    }

    public ModelRateLimitException(String provider, Throwable cause) {
        super("Rate limit exceeded for provider: " + provider, cause);
    }
}
