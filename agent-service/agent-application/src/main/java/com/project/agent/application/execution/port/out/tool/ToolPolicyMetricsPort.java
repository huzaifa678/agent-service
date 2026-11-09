package com.project.agent.application.execution.port.out.tool;

/**
 * Outbound port for tool-authorization observability. Kept free of any metrics-library types
 * so the application layer stays framework-agnostic; the adapter binds it to Micrometer
 * (exported through the service's OpenTelemetry/OTLP pipeline).
 */
public interface ToolPolicyMetricsPort {

    /**
     * Record one tool-authorization decision.
     *
     * @param toolName the tool being adjudicated
     * @param allowed  whether the call was permitted
     */
    void recordDecision(String toolName, boolean allowed);

    /**
     * Record that the decision engine was unreachable and the harness had to fall back to its
     * configured posture.
     *
     * @param toolName the tool that was being adjudicated when the engine failed
     */
    void recordUnavailable(String toolName);
}
