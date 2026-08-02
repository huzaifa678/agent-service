package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a requested model is not supported by the service.
 */
public class UnsupportedModelException
        extends BusinessRuleViolationException {

    public UnsupportedModelException(String model) {
        super("Unsupported model: " + model);
    }

    public UnsupportedModelException(
            String model,
            Throwable cause
    ) {
        super(
                "Unsupported model exception: "
                + model,
                cause
        );
    }
}
