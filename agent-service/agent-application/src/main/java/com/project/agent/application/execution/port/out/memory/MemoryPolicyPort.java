package com.project.agent.application.execution.port.out.memory;

/**
 * Outbound port for the memory-governance decision point. The application asks it whether a
 * given recall or persist is allowed; the adapter decides how — the production adapter ships
 * the request to Open Policy Agent (OPA) and evaluates the {@code agent/memory} Rego policy,
 * so the rules can change without redeploying the service.
 *
 * <p>Implementations that cannot reach the decision engine should raise
 * {@link MemoryPolicyUnavailableException} rather than guessing a verdict; the harness owns
 * the fail-open / fail-closed choice, not the transport.
 */
public interface MemoryPolicyPort {

    /**
     * Adjudicate a single memory access.
     *
     * @throws MemoryPolicyUnavailableException if the decision engine could not be reached
     */
    MemoryDecision decide(MemoryAccessRequest request);
}
