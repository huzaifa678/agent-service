package com.project.agent.application.execution.port.out.tool;

/**
 * Raised by a {@link ToolPolicyPort} when the decision engine cannot be reached or returns an
 * unusable response. It signals "no verdict", not "denied" — the
 * {@link com.project.agent.application.execution.service.harness.AgentToolHarness} translates
 * it into an allow or a deny according to the configured fail-open / fail-closed posture.
 */
public class ToolPolicyUnavailableException extends RuntimeException {

    public ToolPolicyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ToolPolicyUnavailableException(String message) {
        super(message);
    }
}
