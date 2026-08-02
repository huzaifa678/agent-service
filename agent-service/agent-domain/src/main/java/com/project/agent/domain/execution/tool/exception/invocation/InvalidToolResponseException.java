package com.project.agent.domain.execution.tool.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when a tool returns a response that cannot be parsed or is otherwise invalid.
 */
public class InvalidToolResponseException extends DomainException {

    public InvalidToolResponseException(String toolName) {
        super("Invalid response received from tool: " + toolName);
    }

    public InvalidToolResponseException(String toolName, Throwable cause) {
        super("Invalid response received from tool: " + toolName, cause);
    }
}
