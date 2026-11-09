package com.project.agent.adapter.out.metrics;

import com.project.agent.application.execution.port.out.tool.ToolPolicyMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer implementation of {@link ToolPolicyMetricsPort}. Metrics leave the pod through
 * the service's existing OpenTelemetry/OTLP pipeline (same path as the memory and RAG
 * metrics).
 *
 * <p>Meters:
 * <ul>
 *   <li>{@code agent.tools.policy.decisions} — counter tagged {@code tool} and {@code outcome}
 *       (allow/deny); a rising deny rate means the tool policy is actively blocking calls.</li>
 *   <li>{@code agent.tools.policy.unavailable} — counter tagged {@code tool}, incremented
 *       whenever the decision engine was unreachable and the harness fell back to its
 *       configured posture; alert on any sustained rate.</li>
 * </ul>
 */
@Component
public class MicrometerToolPolicyMetricsAdapter implements ToolPolicyMetricsPort {

    private final MeterRegistry registry;

    public MicrometerToolPolicyMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordDecision(String toolName, boolean allowed) {
        Counter.builder("agent.tools.policy.decisions")
                .description("Agent tool-authorization decisions by tool and outcome")
                .tag("tool", toolName)
                .tag("outcome", allowed ? "allow" : "deny")
                .register(registry)
                .increment();
    }

    @Override
    public void recordUnavailable(String toolName) {
        Counter.builder("agent.tools.policy.unavailable")
                .description("Tool-authorization decisions where the policy engine was unreachable")
                .tag("tool", toolName)
                .register(registry)
                .increment();
    }
}
