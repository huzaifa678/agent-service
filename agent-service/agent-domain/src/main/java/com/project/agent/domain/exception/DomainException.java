package com.project.agent.domain.exception;

/**
 * Base type for all domain-layer exceptions. Extends {@link RuntimeException} so
 * business-rule violations propagate without checked-exception noise; adapters translate
 * concrete subtypes into transport errors (e.g. HTTP status codes).
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
