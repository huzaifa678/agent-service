package com.project.agent.application.execution.port.out.guardrail;

/**
 * Outbound port for guardrail observability. Kept free of any metrics-library types so the
 * application layer stays framework-agnostic; the adapter binds it to Micrometer (exported
 * through the service's OpenTelemetry/OTLP pipeline).
 */
public interface GuardrailMetricsPort {

    /**
     * Record one guardrail decision.
     *
     * @param direction the boundary being screened (input or output)
     * @param allowed   whether the content was allowed to pass
     */
    void recordDecision(GuardrailDirection direction, boolean allowed);

    /**
     * Record that the decision engine was unreachable and the harness had to fall back to its
     * configured posture.
     *
     * @param direction the boundary that was being screened when the engine failed
     */
    void recordUnavailable(GuardrailDirection direction);
}
