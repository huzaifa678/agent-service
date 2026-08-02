package com.project.agent.domain.exception;

/**
 * Base class for exceptions raised when a domain invariant or business rule is violated.
 * Adapters typically map subtypes to HTTP 409/422.
 */
public abstract class BusinessRuleViolationException extends DomainException {

    protected BusinessRuleViolationException(String message) {
        super(message);
    }

    protected BusinessRuleViolationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
