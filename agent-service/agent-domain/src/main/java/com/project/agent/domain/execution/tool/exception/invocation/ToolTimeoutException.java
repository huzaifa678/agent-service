package com.project.agent.domain.execution.tool.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when a tool invocation exceeds its execution timeout.
 */
public class ToolTimeoutException extends DomainException {

    public ToolTimeoutException(String toolName) {
        super("Tool execution timed out: " + toolName);
    }
}