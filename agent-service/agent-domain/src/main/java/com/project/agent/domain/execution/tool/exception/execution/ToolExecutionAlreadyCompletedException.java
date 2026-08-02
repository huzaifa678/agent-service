package com.project.agent.domain.execution.tool.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a terminal operation is attempted on an already-completed tool execution.
 */
public class ToolExecutionAlreadyCompletedException
        extends BusinessRuleViolationException {

    public ToolExecutionAlreadyCompletedException() {
        super("Tool execution has already completed.");
    }
}
