package com.project.agent.application.execution.port.out.guardrail;

/**
 * Raised by a {@link GuardrailPolicyPort} when the decision engine cannot be reached or
 * returns an unusable response. It signals "no verdict", not "blocked" — the
 * {@link com.project.agent.application.execution.service.harness.AgentGuardrailHarness}
 * translates it into a pass or a block according to the configured fail-open / fail-closed
 * posture.
 */
public class GuardrailPolicyUnavailableException extends RuntimeException {

    public GuardrailPolicyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public GuardrailPolicyUnavailableException(String message) {
        super(message);
    }
}
