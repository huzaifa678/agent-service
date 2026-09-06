package com.project.agent.application.execution.port.out.rag;

/**
 * Outbound port for emitting RAG retrieval observability signals. Kept free of any
 * metrics-library types so the application layer stays framework-agnostic; the adapter
 * binds it to Micrometer (exported via the service's OpenTelemetry/OTLP pipeline).
 */
public interface RagMetricsPort {

    /**
     * Record one retrieval.
     *
     * @param confidence    retrieval confidence (0–1): strongest kept relevance score,
     *                      or {@code 0} when no passage cleared the relevance bar
     * @param keptPassages  how many passages were injected into the prompt; {@code 0}
     *                      means the grounding guard fired (no reliable context)
     */
    void recordRetrieval(double confidence, int keptPassages);
}
