package com.project.agent.application.execution.port.out.memory;

/**
 * Raised by a {@link MemoryPolicyPort} when the decision engine cannot be reached or returns
 * an unusable response. It signals "no verdict", not "denied" — the harness translates it
 * into an allow or a deny according to the configured fail-open / fail-closed posture.
 */
public class MemoryPolicyUnavailableException extends RuntimeException {

    public MemoryPolicyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemoryPolicyUnavailableException(String message) {
        super(message);
    }
}
