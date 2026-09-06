package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RagRetrievalPolicyTest {

    private final RagRetrievalPolicy policy =
            new RagRetrievalPolicy(20, 0.75, 3);

    @Test
    void gate_dropsBelowThreshold_andCapsAtMaxPassages() {

        List<RetrievedPassage> kept = policy.gate(List.of(
                new RetrievedPassage("a", 0.95),
                new RetrievedPassage("b", 0.80),
                new RetrievedPassage("c", 0.60),   // dropped: below 0.75
                new RetrievedPassage("d", 0.79),
                new RetrievedPassage("e", 0.99)     // dropped: exceeds cap of 3
        ));

        assertThat(kept).extracting(RetrievedPassage::content)
                .containsExactly("a", "b", "d");
    }

    @Test
    void gate_preservesIncomingOrder() {

        // Incoming order is the fusion order, not score order — it must be preserved.
        List<RetrievedPassage> kept = policy.gate(List.of(
                new RetrievedPassage("low-but-first", 0.76),
                new RetrievedPassage("high-but-second", 0.98)
        ));

        assertThat(kept).extracting(RetrievedPassage::content)
                .containsExactly("low-but-first", "high-but-second");
    }

    @Test
    void confidence_isStrongestKeptScore() {

        double confidence = policy.confidence(List.of(
                new RetrievedPassage("a", 0.81),
                new RetrievedPassage("b", 0.92),
                new RetrievedPassage("c", 0.77)
        ));

        assertThat(confidence).isEqualTo(0.92);
    }

    @Test
    void confidence_isZero_whenNothingKept() {
        assertThat(policy.confidence(List.of())).isZero();
    }
}
