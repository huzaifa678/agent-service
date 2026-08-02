package com.project.agent.domain.execution.tool.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when a tool invocation fails.
 */
public class ToolInvocationException extends DomainException {

    public ToolInvocationException(String toolName) {
        super("Tool invocation failed: " + toolName);
    }

    public ToolInvocationException(String toolName, Throwable cause) {
        super("Tool invocation failed: " + toolName, cause);
    }
}
