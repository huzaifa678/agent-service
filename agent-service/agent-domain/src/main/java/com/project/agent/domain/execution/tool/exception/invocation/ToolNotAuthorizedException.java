package com.project.agent.domain.execution.tool.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a tool is <em>supported</em> by the build but not <em>authorized</em> by the
 * tool-governance policy for this context (e.g. a tenant that is not entitled to the
 * {@code code_interpreter} tool). Distinct from
 * {@link UnsupportedToolException}, which is about a tool the service does not implement at
 * all. Surfaced pre-flight when the client asks to enable a tool it may not use.
 */
public class ToolNotAuthorizedException
        extends BusinessRuleViolationException {

    public ToolNotAuthorizedException(String tool) {
        super("Tool not authorized by policy: " + tool);
    }
}
