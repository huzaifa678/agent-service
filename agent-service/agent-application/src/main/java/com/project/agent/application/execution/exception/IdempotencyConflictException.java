package com.project.agent.application.execution.exception;

import com.project.agent.application.shared.exception.ApplicationException;

/**
 * Raised when an idempotent request cannot be satisfied right now: another request with the same
 * {@code Idempotency-Key} is still in progress, or the key was reused with a different request.
 * Adapters map this to HTTP 409 Conflict.
 */
public class IdempotencyConflictException extends ApplicationException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
