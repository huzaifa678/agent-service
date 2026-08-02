package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when a prompt template is missing or cannot be parsed.
 */
public class InvalidPromptTemplateException extends DomainException {

    public InvalidPromptTemplateException(String templateName) {
        super("Invalid prompt template: " + templateName);
    }

    public InvalidPromptTemplateException(String templateName, Throwable cause) {
        super("Invalid prompt template: " + templateName, cause);
    }
}
