package com.project.agent.domain.exception;

/**
 * Base class for exceptions signalling that a referenced aggregate does not exist.
 * Adapters typically map subtypes to HTTP 404.
 */
public abstract class AggregateNotFoundException extends DomainException {

    protected AggregateNotFoundException(String message) {
        super(message);
    }
}
