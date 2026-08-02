package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when an assembled prompt exceeds the maximum supported size.
 */
public class PromptTooLargeException extends BusinessRuleViolationException {

    public PromptTooLargeException() {
        super("Prompt exceeds the maximum supported size.");
    }

    public PromptTooLargeException(int size, int maxSize) {
        super(
                "Prompt size of " + size +
                        " exceeds the maximum allowed size of " + maxSize + "."
        );
    }
}
