package com.project.agent.adapter.out.policy;

import com.project.agent.application.execution.port.out.memory.MemoryAccessRequest;
import com.project.agent.application.execution.port.out.memory.MemoryDecision;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link MemoryPolicyPort} used when memory governance is switched off
 * ({@code agent.memory.policy.enabled=false}, the default). It permits every access, so the
 * harness is a transparent pass-through in local/dev runs and any environment without an OPA
 * sidecar. Flip the flag to {@code true} to activate {@link OpaMemoryPolicyAdapter} instead.
 */
@Component
@ConditionalOnProperty(name = "agent.memory.policy.enabled", havingValue = "false", matchIfMissing = true)
public class AllowAllMemoryPolicyAdapter implements MemoryPolicyPort {

    @Override
    public MemoryDecision decide(MemoryAccessRequest request) {
        return MemoryDecision.allow();
    }
}
