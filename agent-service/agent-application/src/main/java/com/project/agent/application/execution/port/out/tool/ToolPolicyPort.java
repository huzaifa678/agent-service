package com.project.agent.application.execution.port.out.tool;

/**
 * Outbound port for the tool-authorization decision point. Before the agent runs a tool the
 * application asks this port whether the call is permitted; the adapter decides how — the
 * production adapter ships the request to Open Policy Agent (OPA) and evaluates the
 * {@code agent/tools} Rego policy, so the allow-list (and any per-tenant or per-argument
 * constraint) can change without redeploying the service.
 *
 * <p>Tools are the agent's highest-risk action surface: {@code web_search} reaches the
 * internet, {@code code_interpreter} runs code. Relevance gating and prompt validation say
 * nothing about whether a <em>tool</em> may fire, which is exactly the kind of governance
 * question that belongs in policy rather than a hard-coded {@code Set} in code.
 *
 * <p>Implementations that cannot reach the decision engine should raise
 * {@link ToolPolicyUnavailableException} rather than guessing a verdict; the
 * {@link com.project.agent.application.execution.service.harness.AgentToolHarness} owns the
 * fail-open / fail-closed choice, not the transport.
 */
public interface ToolPolicyPort {

    /**
     * Adjudicate a single tool invocation.
     *
     * @throws ToolPolicyUnavailableException if the decision engine could not be reached
     */
    ToolDecision decide(ToolAccessRequest request);
}
