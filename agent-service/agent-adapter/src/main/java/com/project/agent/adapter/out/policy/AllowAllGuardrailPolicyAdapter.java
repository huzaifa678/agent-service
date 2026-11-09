package com.project.agent.adapter.out.policy;

import com.project.agent.application.execution.port.out.guardrail.GuardrailDecision;
import com.project.agent.application.execution.port.out.guardrail.GuardrailPolicyPort;
import com.project.agent.application.execution.port.out.guardrail.GuardrailRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link GuardrailPolicyPort} used when I/O guardrails are switched off
 * ({@code agent.guardrail.enabled=false}, the default). It passes every check, so the
 * {@link com.project.agent.application.execution.service.harness.AgentGuardrailHarness} is a
 * transparent pass-through in local/dev runs and any environment without an OPA sidecar. Flip
 * the flag to {@code true} to activate {@link OpaGuardrailPolicyAdapter} instead.
 */
@Component
@ConditionalOnProperty(name = "agent.guardrail.enabled", havingValue = "false", matchIfMissing = true)
public class AllowAllGuardrailPolicyAdapter implements GuardrailPolicyPort {

    @Override
    public GuardrailDecision decide(GuardrailRequest request) {
        return GuardrailDecision.allow();
    }
}
