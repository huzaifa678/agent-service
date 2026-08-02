package com.project.agent.domain.execution.agent.exception.prompt;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when the assembled conversation exceeds the model's context window.
 */
public class ContextWindowExceededException extends BusinessRuleViolationException {

    public ContextWindowExceededException() {
        super("Conversation exceeds the model context window.");
    }

    public ContextWindowExceededException(String model) {
        super(
                "Conversation exceeds the context window for model: "
                        + model
        );
    }

    public ContextWindowExceededException(
            String model,
            Throwable cause
    ) {
        super(
                "Conversation exceeds the context window for model: "
                        + model,
                cause
        );
    }
}
