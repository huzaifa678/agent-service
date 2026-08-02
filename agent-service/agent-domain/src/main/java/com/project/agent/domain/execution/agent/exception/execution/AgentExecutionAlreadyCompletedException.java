package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a terminal operation is attempted on an already-completed agent execution.
 */
public class AgentExecutionAlreadyCompletedException
        extends BusinessRuleViolationException {

    public AgentExecutionAlreadyCompletedException() {
        super("Agent execution has already completed.");
    }
}
