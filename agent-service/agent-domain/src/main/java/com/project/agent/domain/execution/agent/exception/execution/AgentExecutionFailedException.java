package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.DomainException;

/**
 * Raised when an agent execution fails and cannot be completed.
 */
public class AgentExecutionFailedException extends DomainException {

    public AgentExecutionFailedException(String message) {
        super(message);
    }

    public AgentExecutionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
