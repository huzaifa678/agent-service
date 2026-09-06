package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.rag.RagMetricsPort;
import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.application.execution.service.harness.AgentMemoryHarness;
import com.project.agent.domain.execution.agent.exception.ConversationNotFoundInVectorStoreException;
import com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException;
import com.project.agent.domain.execution.agent.exception.UnsupportedEmbeddingModelException;
import com.project.agent.domain.execution.agent.exception.VectorSearchException;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.identity.ConversationId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides Retrieval-Augmented Generation (RAG) support for agent executions.
 *
 * <p>Retrieves relevant context before an LLM invocation and indexes assistant
 * responses after successful completion.
 *
 * <p>Reliability guards applied on retrieval:
 * <ul>
 *   <li><b>Score gating</b> — candidates below the configured similarity threshold are
 *       dropped ({@link RagRetrievalPolicy}), so low-relevance passages never enter the
 *       prompt (the most common cause of grounded-model hallucination).</li>
 *   <li><b>Grounding guard</b> — when nothing clears the threshold the agent is explicitly
 *       told no reliable context was found, so it can set {@code data_sufficient=false}
 *       and lower its own confidence rather than inventing an answer.</li>
 *   <li><b>Per-passage relevance</b> — kept passages are annotated with their score so the
 *       model can weigh them.</li>
 *   <li><b>Memory governance</b> — relevance-gated passages pass through
 *       {@link AgentMemoryHarness} before they enter the prompt, and assistant messages pass
 *       through it before they are written to long-term memory. The harness enforces an
 *       OPA/Rego policy, so what the agent may remember and recall is governed as code
 *       rather than hard-wired here.</li>
 * </ul>
 *
 * <p>Retrieval is best-effort: retrieval failures degrade to the grounding guard rather
 * than failing the execution. Configuration errors (unsupported embedding model) surface.
 */
@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String GROUNDING_GUARD =
            "No relevant background context was retrieved for this question. "
                    + "Answer only from the conversation history above. If it is not enough "
                    + "to answer confidently, set \"data_sufficient\" to false, set "
                    + "\"confidence\" to \"low\", and state what information is missing.";

    private final VectorStorePort vectorStore;

    private final RagRetrievalPolicy policy;

    private final RagMetricsPort metrics;

    private final AgentMemoryHarness harness;

    /**
     * Retrieves relevant passages from the vector store, gates them by relevance, and
     * returns the SYSTEM context messages to inject (survivors plus a grounding header,
     * or a guard when none survive) together with the retrieval confidence. The
     * confidence is logged, emitted as a metric, and persisted on the execution.
     */
    public RagRetrievalResult retrieveContext(
            ConversationId conversationId,
            String query
    ) {

        List<RetrievedPassage> candidates =
                searchQuietly(conversationId, query);

        // Relevance gating decides what is worth recalling; the harness decides what is
        // allowed to be recalled (secret redaction, tenant isolation, retention) via policy.
        List<RetrievedPassage> kept =
                harness.guardRecall(conversationId, policy.gate(candidates));

        double confidence =
                policy.confidence(kept);

        log.info(
                "RAG retrieval for conversation {}: candidates={}, kept={}, confidence={}",
                conversationId,
                candidates.size(),
                kept.size(),
                String.format("%.2f", confidence)
        );

        metrics.recordRetrieval(confidence, kept.size());

        return new RagRetrievalResult(
                toContextMessages(kept, confidence),
                confidence
        );
    }

    private List<RetrievedPassage> searchQuietly(
            ConversationId conversationId,
            String query
    ) {

        try {

            return vectorStore.search(
                    conversationId,
                    query,
                    policy.candidatePoolSize()
            );

        } catch (ConversationNotFoundInVectorStoreException e) {

            // First execution. Nothing indexed yet.
            return List.of();

        } catch (VectorSearchException e) {

            // Retrieval is best effort; fall through to the grounding guard.
            log.warn(
                    "Vector search failed for conversation {}; proceeding without context.",
                    conversationId,
                    e
            );
            return List.of();
        }
    }

    private List<ChatMessage> toContextMessages(
            List<RetrievedPassage> kept,
            double confidence
    ) {

        if (kept.isEmpty()) {
            return List.of(
                    new ChatMessage(MessageRole.SYSTEM, GROUNDING_GUARD)
            );
        }

        List<ChatMessage> context = new ArrayList<>();

        context.add(
                new ChatMessage(
                        MessageRole.SYSTEM,
                        String.format(
                                "Retrieved %d background context passage(s) with retrieval "
                                        + "confidence %.2f (0-1 scale). Ground your answer in "
                                        + "them. If they do not actually address the question, "
                                        + "rely on the conversation history and set "
                                        + "\"data_sufficient\" to false.",
                                kept.size(),
                                confidence
                        )
                )
        );

        for (RetrievedPassage passage : kept) {
            context.add(
                    new ChatMessage(
                            MessageRole.SYSTEM,
                            String.format(
                                    "[retrieved context • relevance %.2f]%n%s",
                                    passage.score(),
                                    passage.content()
                            )
                    )
            );
        }

        return context;
    }

    /**
     * Indexes the assistant response so it becomes available for future
     * retrieval operations.
     */
    public void indexAssistantResponse(
            ConversationId conversationId,
            Message assistantMessage
    ) {

        String content = assistantMessage.getContent().value();

        if (!harness.guardPersist(conversationId, content)) {
            // Policy vetoed long-term retention of this content; skip indexing.
            return;
        }

        try {

            vectorStore.index(
                    conversationId,
                    assistantMessage.getId(),
                    content
            );

        } catch (UnsupportedEmbeddingModelException e) {
            throw e;

        } catch (EmbeddingGenerationException e) {
            // Indexing is best effort; a failure must not fail the execution.
            log.warn(
                    "Failed to index assistant message {} for conversation {}.",
                    assistantMessage.getId(),
                    conversationId,
                    e
            );
        }
    }
}
