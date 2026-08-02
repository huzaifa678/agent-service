package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when a request to the LLM model times out.
 */
public class ModelTimeoutException extends DomainException {

    public ModelTimeoutException(String model) {
        super("Model request timed out: " + model);
    }

    public ModelTimeoutException(String model, Throwable cause) {
        super("Model request timed out: " + model, cause);
    }
}