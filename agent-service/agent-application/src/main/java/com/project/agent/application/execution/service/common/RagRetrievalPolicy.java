package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns raw vector-store candidates into the passages actually worth trusting, and
 * derives a retrieval-confidence signal from their similarity scores.
 *
 * <p>Scores are langchain4j similarity scores, {@code (1 + cosine) / 2} on a 0–1 scale.
 * A candidate is kept only if its score is at least {@code min-score}; at most
 * {@code max-passages} are kept, preserving the incoming order (which for the hybrid
 * adapter is Reciprocal-Rank-Fusion order). Retrieval confidence is the strongest kept
 * score, or {@code 0} when nothing clears the bar — the signal the agent uses to decide
 * whether it has enough grounding to answer.
 */
@Component
public class RagRetrievalPolicy {

    private final int candidatePoolSize;
    private final double minScore;
    private final int maxPassages;

    public RagRetrievalPolicy(
            @Value("${agent.rag.candidate-pool-size:20}") int candidatePoolSize,
            @Value("${agent.rag.min-score:0.75}") double minScore,
            @Value("${agent.rag.max-passages:5}") int maxPassages
    ) {
        this.candidatePoolSize = candidatePoolSize;
        this.minScore = minScore;
        this.maxPassages = maxPassages;
    }

    /** How many candidates to over-fetch from the store before gating. */
    public int candidatePoolSize() {
        return candidatePoolSize;
    }

    /** Keep only sufficiently-similar candidates, capped at {@code max-passages}. */
    public List<RetrievedPassage> gate(List<RetrievedPassage> candidates) {
        return candidates.stream()
                .filter(passage -> passage.score() >= minScore)
                .limit(maxPassages)
                .toList();
    }

    /** Retrieval confidence: the strongest kept score, or {@code 0} when none were kept. */
    public double confidence(List<RetrievedPassage> keptPassages) {
        return keptPassages.stream()
                .mapToDouble(RetrievedPassage::score)
                .max()
                .orElse(0.0);
    }
}
