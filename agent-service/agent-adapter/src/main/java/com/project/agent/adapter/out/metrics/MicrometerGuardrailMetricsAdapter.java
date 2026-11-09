package com.project.agent.adapter.out.metrics;

import com.project.agent.application.execution.port.out.guardrail.GuardrailDirection;
import com.project.agent.application.execution.port.out.guardrail.GuardrailMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer implementation of {@link GuardrailMetricsPort}. Metrics leave the pod through the
 * service's existing OpenTelemetry/OTLP pipeline (same path as the memory, tool, and RAG
 * metrics).
 *
 * <p>Meters:
 * <ul>
 *   <li>{@code agent.guardrail.decisions} — counter tagged {@code direction} (input/output)
 *       and {@code outcome} (allow/deny); a rising deny rate on input means injection attempts
 *       are being blocked, on output that answers are being withheld.</li>
 *   <li>{@code agent.guardrail.unavailable} — counter tagged {@code direction}, incremented
 *       whenever the decision engine was unreachable and the harness fell back to its
 *       configured posture; alert on any sustained rate.</li>
 * </ul>
 */
@Component
public class MicrometerGuardrailMetricsAdapter implements GuardrailMetricsPort {

    private final MeterRegistry registry;

    public MicrometerGuardrailMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordDecision(GuardrailDirection direction, boolean allowed) {
        Counter.builder("agent.guardrail.decisions")
                .description("Agent I/O guardrail decisions by direction and outcome")
                .tag("direction", direction.name().toLowerCase())
                .tag("outcome", allowed ? "allow" : "deny")
                .register(registry)
                .increment();
    }

    @Override
    public void recordUnavailable(GuardrailDirection direction) {
        Counter.builder("agent.guardrail.unavailable")
                .description("Guardrail decisions where the policy engine was unreachable")
                .tag("direction", direction.name().toLowerCase())
                .register(registry)
                .increment();
    }
}
