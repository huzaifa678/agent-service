package com.project.agent.adapter.out.metrics;

import com.project.agent.application.execution.port.out.memory.MemoryOperation;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer implementation of {@link MemoryPolicyMetricsPort}. Metrics leave the pod through
 * the service's existing OpenTelemetry/OTLP pipeline (same path as the RAG metrics).
 *
 * <p>Meters:
 * <ul>
 *   <li>{@code agent.memory.policy.decisions} — counter tagged {@code operation}
 *       (recall/persist) and {@code outcome} (allow/deny); a rising deny rate on recall means
 *       the policy is redacting remembered context, on persist that it is refusing writes.</li>
 *   <li>{@code agent.memory.policy.unavailable} — counter tagged {@code operation}, incremented
 *       whenever the decision engine could not be reached and the harness fell back to its
 *       configured posture; alert on any sustained rate.</li>
 * </ul>
 */
@Component
public class MicrometerMemoryPolicyMetricsAdapter implements MemoryPolicyMetricsPort {

    private final MeterRegistry registry;

    public MicrometerMemoryPolicyMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordDecision(MemoryOperation operation, boolean allowed) {
        Counter.builder("agent.memory.policy.decisions")
                .description("Agent memory-governance decisions by operation and outcome")
                .tag("operation", operation.name().toLowerCase())
                .tag("outcome", allowed ? "allow" : "deny")
                .register(registry)
                .increment();
    }

    @Override
    public void recordUnavailable(MemoryOperation operation) {
        Counter.builder("agent.memory.policy.unavailable")
                .description("Memory-governance decisions where the policy engine was unreachable")
                .tag("operation", operation.name().toLowerCase())
                .register(registry)
                .increment();
    }
}
