package com.project.agent.application.execution.port.out.memory;

/**
 * Outbound port for memory-governance observability. Kept free of any metrics-library types
 * so the application layer stays framework-agnostic; the adapter binds it to Micrometer
 * (exported through the service's OpenTelemetry/OTLP pipeline).
 */
public interface MemoryPolicyMetricsPort {

    /**
     * Record one policy decision.
     *
     * @param operation the boundary being governed (recall or persist)
     * @param allowed   whether the access was permitted
     */
    void recordDecision(MemoryOperation operation, boolean allowed);

    /**
     * Record that the decision engine was unreachable and the harness had to fall back to its
     * configured posture.
     *
     * @param operation the boundary that was being governed when the engine failed
     */
    void recordUnavailable(MemoryOperation operation);
}
