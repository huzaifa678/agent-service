package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.memory.MemoryAccessRequest;
import com.project.agent.application.execution.port.out.memory.MemoryDecision;
import com.project.agent.application.execution.port.out.memory.MemoryOperation;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyMetricsPort;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyPort;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyUnavailableException;
import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentMemoryHarnessTest {

    private final ConversationId conversationId = ConversationId.of(UUID.randomUUID());

    @Mock
    private MemoryPolicyPort policy;

    @Mock
    private MemoryPolicyMetricsPort metrics;

    @Test
    void guardRecall_dropsPassagesThePolicyDenies_preservingOrder() {

        AgentMemoryHarness harness = new AgentMemoryHarness(policy, metrics, false);

        RetrievedPassage keep1 = new RetrievedPassage("keep one", 0.95);
        RetrievedPassage drop = new RetrievedPassage("leaked secret", 0.90);
        RetrievedPassage keep2 = new RetrievedPassage("keep two", 0.80);

        when(policy.decide(any())).thenAnswer(invocation -> {
            MemoryAccessRequest request = invocation.getArgument(0);
            return request.content().contains("secret")
                    ? MemoryDecision.deny("resembles a secret")
                    : MemoryDecision.allow();
        });

        List<RetrievedPassage> result =
                harness.guardRecall(conversationId, List.of(keep1, drop, keep2));

        assertThat(result).containsExactly(keep1, keep2);
        verify(metrics).recordDecision(MemoryOperation.RECALL, false);
    }

    @Test
    void guardPersist_returnsFalse_whenPolicyDenies() {

        AgentMemoryHarness harness = new AgentMemoryHarness(policy, metrics, false);

        when(policy.decide(any()))
                .thenReturn(MemoryDecision.deny("tenant retention is none"));

        assertThat(harness.guardPersist(conversationId, "an ordinary answer")).isFalse();
        verify(metrics).recordDecision(MemoryOperation.PERSIST, false);
    }

    @Test
    void guardPersist_returnsTrue_whenPolicyAllows() {

        AgentMemoryHarness harness = new AgentMemoryHarness(policy, metrics, false);

        when(policy.decide(any())).thenReturn(MemoryDecision.allow());

        assertThat(harness.guardPersist(conversationId, "an ordinary answer")).isTrue();
        verify(metrics).recordDecision(MemoryOperation.PERSIST, true);
    }

    @Test
    void failClosed_dropsRecallAndSkipsPersist_whenEngineUnavailable() {

        AgentMemoryHarness harness = new AgentMemoryHarness(policy, metrics, false);

        when(policy.decide(any()))
                .thenThrow(new MemoryPolicyUnavailableException("opa down"));

        List<RetrievedPassage> recalled = harness.guardRecall(
                conversationId, List.of(new RetrievedPassage("something", 0.99)));

        assertThat(recalled).isEmpty();
        assertThat(harness.guardPersist(conversationId, "answer")).isFalse();
        verify(metrics, org.mockito.Mockito.times(2))
                .recordUnavailable(any());
    }

    @Test
    void failOpen_keepsRecallAndAllowsPersist_whenEngineUnavailable() {

        AgentMemoryHarness harness = new AgentMemoryHarness(policy, metrics, true);

        when(policy.decide(any()))
                .thenThrow(new MemoryPolicyUnavailableException("opa down"));

        RetrievedPassage passage = new RetrievedPassage("something", 0.99);

        assertThat(harness.guardRecall(conversationId, List.of(passage)))
                .containsExactly(passage);
        assertThat(harness.guardPersist(conversationId, "answer")).isTrue();
    }
}
