package com.project.agent.domain.execution.tool.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when an agent requests a tool that is not registered/supported.
 */
public class UnsupportedToolException
        extends BusinessRuleViolationException {

    public UnsupportedToolException(String tool) {
        super("Unsupported tool: " + tool);
    }
}
