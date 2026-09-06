package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.memory.MemoryDecision;
import com.project.agent.application.execution.port.out.memory.MemoryOperation;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyMetricsPort;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyPort;
import com.project.agent.application.execution.port.out.rag.RagMetricsPort;
import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.application.execution.service.common.builder.MessageBuilder;
import com.project.agent.application.execution.service.harness.AgentMemoryHarness;
import com.project.agent.domain.execution.agent.exception.ConversationNotFoundInVectorStoreException;
import com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException;
import com.project.agent.domain.execution.agent.exception.UnsupportedEmbeddingModelException;
import com.project.agent.domain.execution.agent.exception.VectorSearchException;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    // Mirrors the defaults on RagRetrievalPolicy.
    private static final int CANDIDATE_POOL_SIZE = 20;
    private static final double MIN_SCORE = 0.75;
    private static final int MAX_PASSAGES = 5;

    @Mock
    private VectorStorePort vectorStore;

    @Mock
    private RagMetricsPort metrics;

    private RagService service;

    @BeforeEach
    void setUp() {
        RagRetrievalPolicy policy =
                new RagRetrievalPolicy(CANDIDATE_POOL_SIZE, MIN_SCORE, MAX_PASSAGES);

        // These tests cover relevance gating; run the harness with an allow-all policy so it
        // is a transparent pass-through. Governance behaviour is covered in AgentMemoryHarnessTest.
        AgentMemoryHarness harness =
                new AgentMemoryHarness(allowAllPolicy(), noopMemoryMetrics(), false);

        service = new RagService(vectorStore, policy, metrics, harness);
    }

    private static MemoryPolicyPort allowAllPolicy() {
        return request -> MemoryDecision.allow();
    }

    private static MemoryPolicyMetricsPort noopMemoryMetrics() {
        return new MemoryPolicyMetricsPort() {
            @Override
            public void recordDecision(MemoryOperation operation, boolean allowed) {
            }

            @Override
            public void recordUnavailable(MemoryOperation operation) {
            }
        };
    }

    @Test
    void retrieveContext_keepsRelevantPassages_withGroundingHeaderAndScores() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(conversationId, "billing", CANDIDATE_POOL_SIZE))
                .thenReturn(List.of(
                        new RetrievedPassage("Passage one", 0.95),
                        new RetrievedPassage("Passage two", 0.88)
                ));

        List<ChatMessage> result =
                service.retrieveContext(conversationId, "billing").contextMessages();

        // 1 grounding header + 2 kept passages.
        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(message ->
                assertThat(message.role()).isEqualTo(MessageRole.SYSTEM));
        assertThat(result.get(0).content())
                .contains("Retrieved 2 background context passage(s)")
                .contains("0.95"); // retrieval confidence == strongest kept score
        assertThat(result.get(1).content()).contains("Passage one").contains("0.95");
        assertThat(result.get(2).content()).contains("Passage two").contains("0.88");
    }

    @Test
    void retrieveContext_dropsLowScorePassages() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(conversationId, "billing", CANDIDATE_POOL_SIZE))
                .thenReturn(List.of(
                        new RetrievedPassage("Relevant", 0.90),
                        new RetrievedPassage("Barely related", 0.60),   // below MIN_SCORE
                        new RetrievedPassage("Unrelated", 0.10)          // below MIN_SCORE
                ));

        List<ChatMessage> result =
                service.retrieveContext(conversationId, "billing").contextMessages();

        // header + only the single passage that cleared the threshold.
        assertThat(result).hasSize(2);
        assertThat(result.get(1).content()).contains("Relevant");
        assertThat(result).noneSatisfy(m ->
                assertThat(m.content()).contains("Unrelated"));
    }

    @Test
    void retrieveContext_returnsGroundingGuard_whenNoPassageClearsThreshold() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(conversationId, "billing", CANDIDATE_POOL_SIZE))
                .thenReturn(List.of(
                        new RetrievedPassage("Weak", 0.40),
                        new RetrievedPassage("Weaker", 0.20)
                ));

        List<ChatMessage> result =
                service.retrieveContext(conversationId, "billing").contextMessages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo(MessageRole.SYSTEM);
        assertThat(result.get(0).content())
                .contains("No relevant background context")
                .contains("data_sufficient");
    }

    @Test
    void retrieveContext_returnsGroundingGuard_whenConversationNotIndexed() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(any(), anyString(), anyInt()))
                .thenThrow(new ConversationNotFoundInVectorStoreException(
                        conversationId.toString()));

        List<ChatMessage> result =
                service.retrieveContext(conversationId, "billing").contextMessages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).contains("No relevant background context");
    }

    @Test
    void retrieveContext_returnsGroundingGuard_whenVectorSearchFails() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(any(), anyString(), anyInt()))
                .thenThrow(new VectorSearchException("Search failed"));

        List<ChatMessage> result =
                service.retrieveContext(conversationId, "billing").contextMessages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).contains("No relevant background context");
    }

    @Test
    void retrieveContext_returnsConfidenceAndRecordsMetric() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(conversationId, "billing", CANDIDATE_POOL_SIZE))
                .thenReturn(List.of(
                        new RetrievedPassage("Relevant", 0.91),
                        new RetrievedPassage("Also relevant", 0.83)
                ));

        RagRetrievalResult result = service.retrieveContext(conversationId, "billing");

        // Confidence is the strongest kept score; metric records confidence + kept count.
        assertThat(result.confidence()).isEqualTo(0.91);
        verify(metrics).recordRetrieval(0.91, 2);
    }

    @Test
    void retrieveContext_recordsZeroConfidence_whenGuardFires() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(conversationId, "billing", CANDIDATE_POOL_SIZE))
                .thenReturn(List.of(new RetrievedPassage("Weak", 0.30)));

        RagRetrievalResult result = service.retrieveContext(conversationId, "billing");

        assertThat(result.confidence()).isZero();
        verify(metrics).recordRetrieval(0.0, 0);
    }

    @Test
    void indexAssistantResponse_indexesMessage() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        Message assistant =
                MessageBuilder.aMessage().withContent("Hello from assistant").build();

        assertDoesNotThrow(() ->
                service.indexAssistantResponse(conversationId, assistant));

        verify(vectorStore).index(
                conversationId,
                assistant.getId(),
                "Hello from assistant"
        );
    }

    @Test
    void indexAssistantResponse_propagatesUnsupportedEmbeddingModelException() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        Message assistant = MessageBuilder.aMessage().build();

        doThrow(new UnsupportedEmbeddingModelException("bge-large"))
                .when(vectorStore)
                .index(any(), any(), anyString());

        assertThrows(
                UnsupportedEmbeddingModelException.class,
                () -> service.indexAssistantResponse(conversationId, assistant)
        );
    }

    @Test
    void indexAssistantResponse_ignoresEmbeddingGenerationException() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        Message assistant = MessageBuilder.aMessage().build();

        doThrow(new EmbeddingGenerationException("Embedding failed"))
                .when(vectorStore)
                .index(any(), any(), anyString());

        assertDoesNotThrow(() ->
                service.indexAssistantResponse(conversationId, assistant));
    }
}
