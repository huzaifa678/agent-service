package com.project.agent.application.execution.port.out.guardrail;

/**
 * Outbound port for the input/output guardrail decision point. The application asks it whether
 * a user message may enter the prompt and whether an assistant answer may be returned; the
 * adapter decides how — the production adapter ships the request to Open Policy Agent (OPA)
 * and evaluates the {@code agent/io} Rego policy, so the rules (prompt-injection patterns,
 * leakage screening, a minimum-grounding floor) can change without redeploying the service.
 *
 * <p>This is the third boundary the harness guards, alongside memory ({@code MemoryPolicyPort})
 * and tools ({@code ToolPolicyPort}): the memory boundary governs what is <em>remembered</em>,
 * the tool boundary what the agent may <em>do</em>, and this boundary what the user may
 * <em>say to</em> and the model may <em>say back from</em> the agent.
 *
 * <p>Implementations that cannot reach the decision engine should raise
 * {@link GuardrailPolicyUnavailableException} rather than guessing a verdict; the
 * {@link com.project.agent.application.execution.service.harness.AgentGuardrailHarness} owns
 * the fail-open / fail-closed choice.
 */
public interface GuardrailPolicyPort {

    /**
     * Adjudicate a single input or output check.
     *
     * @throws GuardrailPolicyUnavailableException if the decision engine could not be reached
     */
    GuardrailDecision decide(GuardrailRequest request);
}
