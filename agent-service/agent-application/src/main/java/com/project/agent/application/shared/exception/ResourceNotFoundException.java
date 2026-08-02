package com.project.agent.application.shared.exception;

/**
 * Raised by query/command services when an aggregate referenced by a command or
 * query does not exist. Adapters typically map this to HTTP 404.
 */
public abstract class ResourceNotFoundException extends ApplicationException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
