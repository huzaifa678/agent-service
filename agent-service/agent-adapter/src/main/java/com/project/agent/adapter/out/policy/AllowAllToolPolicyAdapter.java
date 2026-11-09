package com.project.agent.adapter.out.policy;

import com.project.agent.application.execution.port.out.tool.ToolAccessRequest;
import com.project.agent.application.execution.port.out.tool.ToolDecision;
import com.project.agent.application.execution.port.out.tool.ToolPolicyPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link ToolPolicyPort} used when tool governance is switched off
 * ({@code agent.tools.policy.enabled=false}, the default). It authorizes every tool, so the
 * {@link com.project.agent.application.execution.service.harness.AgentToolHarness} is a
 * transparent pass-through in local/dev runs and any environment without an OPA sidecar. Flip
 * the flag to {@code true} to activate {@link OpaToolPolicyAdapter} instead.
 */
@Component
@ConditionalOnProperty(name = "agent.tools.policy.enabled", havingValue = "false", matchIfMissing = true)
public class AllowAllToolPolicyAdapter implements ToolPolicyPort {

    @Override
    public ToolDecision decide(ToolAccessRequest request) {
        return ToolDecision.allow();
    }
}
