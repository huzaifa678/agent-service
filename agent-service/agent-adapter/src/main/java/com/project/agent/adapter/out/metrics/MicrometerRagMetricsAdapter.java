package com.project.agent.adapter.out.metrics;

import com.project.agent.application.execution.port.out.rag.RagMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer implementation of {@link RagMetricsPort}. Metrics are exported through the
 * service's existing OpenTelemetry/OTLP pipeline (micrometer-registry-otlp) — there is no
 * Prometheus scrape of this app; the cluster's Prometheus scrapes Istio mesh metrics.
 *
 * <p>Meters:
 * <ul>
 *   <li>{@code agent.rag.retrieval.confidence} — distribution of retrieval confidence
 *       (0–1); alert on a falling mean/quantile.</li>
 *   <li>{@code agent.rag.retrieval.kept_passages} — distribution of how many passages
 *       were injected.</li>
 *   <li>{@code agent.rag.retrieval.grounding_guard} — counter of retrievals where nothing
 *       cleared the relevance bar (the model was told it had no reliable context); alert
 *       on a rising rate.</li>
 * </ul>
 */
@Component
public class MicrometerRagMetricsAdapter implements RagMetricsPort {

    private final DistributionSummary confidenceSummary;
    private final DistributionSummary keptPassagesSummary;
    private final Counter groundingGuardCounter;

    public MicrometerRagMetricsAdapter(MeterRegistry registry) {

        this.confidenceSummary = DistributionSummary.builder("agent.rag.retrieval.confidence")
                .description("RAG retrieval confidence (0-1): strongest kept relevance score")
                .publishPercentiles(0.5, 0.95)
                .register(registry);

        this.keptPassagesSummary = DistributionSummary.builder("agent.rag.retrieval.kept_passages")
                .description("Number of context passages injected into the prompt after gating")
                .baseUnit("passages")
                .register(registry);

        this.groundingGuardCounter = Counter.builder("agent.rag.retrieval.grounding_guard")
                .description("Retrievals where no passage cleared the relevance bar (no reliable context)")
                .register(registry);
    }

    @Override
    public void recordRetrieval(double confidence, int keptPassages) {

        confidenceSummary.record(confidence);
        keptPassagesSummary.record(keptPassages);

        if (keptPassages == 0) {
            groundingGuardCounter.increment();
        }
    }
}
