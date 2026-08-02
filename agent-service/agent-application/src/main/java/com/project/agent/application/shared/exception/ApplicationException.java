package com.project.agent.application.shared.exception;

/**
 * Base type for failures that originate in the application (use-case) layer, as
 * opposed to {@link com.project.agent.domain.exception.DomainException} which
 * expresses violations of domain invariants. Application exceptions describe
 * orchestration-level problems — a missing aggregate, an invalid command, a
 * failed downstream call the use case chose to surface.
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
