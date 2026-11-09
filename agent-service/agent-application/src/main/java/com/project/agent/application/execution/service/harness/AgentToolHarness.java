package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.tool.ToolAccessRequest;
import com.project.agent.application.execution.port.out.tool.ToolDecision;
import com.project.agent.application.execution.port.out.tool.ToolPolicyMetricsPort;
import com.project.agent.application.execution.port.out.tool.ToolPolicyPort;
import com.project.agent.application.execution.port.out.tool.ToolPolicyUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The governance envelope around the agent's tool use — the second boundary the harness
 * guards, alongside memory ({@link AgentMemoryHarness}).
 *
 * <p>Tools are the agent's highest-risk action surface: a tool reaches outside the model to
 * search the web, run code, or call other systems. {@code ToolExecutionService} already
 * rejects tools that are not <em>supported</em> by the build; this harness decides whether a
 * supported tool is <em>authorized</em> in this context — a separate question, and one that
 * changes per tenant and for compliance reasons, so it belongs to policy ({@link ToolPolicyPort},
 * backed in production by an OPA/Rego bundle) rather than a hard-coded set.
 *
 * <p>When the decision engine is unreachable the harness applies a configured posture:
 * <ul>
 *   <li><b>fail-closed</b> (default) — deny the tool call. The safe default: an ungoverned
 *       side-effecting tool is worse than a turn that answers without it.</li>
 *   <li><b>fail-open</b> — allow the tool call, for deployments where availability beats
 *       strict tool governance.</li>
 * </ul>
 */
@Component
public class AgentToolHarness {

    private static final Logger log = LoggerFactory.getLogger(AgentToolHarness.class);

    private final ToolPolicyPort policy;
    private final ToolPolicyMetricsPort metrics;
    private final boolean failOpen;

    public AgentToolHarness(
            ToolPolicyPort policy,
            ToolPolicyMetricsPort metrics,
            @Value("${agent.tools.policy.fail-open:false}") boolean failOpen
    ) {
        this.policy = policy;
        this.metrics = metrics;
        this.failOpen = failOpen;
    }

    /**
     * Decide whether a tool the client asked to enable may be offered to the model this turn.
     * Evaluated before the LLM call, so a tool a tenant is not entitled to never reaches the
     * prompt as an available function.
     *
     * @return {@code true} if the tool may be enabled
     */
    public boolean authorizeEnablement(String toolName) {
        return permitted(ToolAccessRequest.forEnablement(toolName, Map.of()), toolName);
    }

    /**
     * Decide whether a concrete tool call the model requested may actually run.
     *
     * @return {@code true} if the caller should execute the tool
     */
    public boolean authorizeInvocation(String toolName, String arguments) {
        boolean allowed =
                permitted(ToolAccessRequest.forInvocation(toolName, arguments, Map.of()), toolName);
        if (!allowed) {
            log.info("Tool policy blocked invocation of '{}'.", toolName);
        }
        return allowed;
    }

    private boolean permitted(ToolAccessRequest request, String toolName) {

        try {

            ToolDecision decision = policy.decide(request);
            metrics.recordDecision(toolName, decision.allowed());

            if (!decision.allowed()) {
                log.debug("Tool '{}' denied: {}", toolName, decision.reason());
            }
            return decision.allowed();

        } catch (ToolPolicyUnavailableException e) {

            metrics.recordUnavailable(toolName);
            log.warn(
                    "Tool policy engine unavailable for '{}'; falling back to fail-{}.",
                    toolName,
                    failOpen ? "open (allow)" : "closed (deny)",
                    e
            );
            return failOpen;
        }
    }
}
