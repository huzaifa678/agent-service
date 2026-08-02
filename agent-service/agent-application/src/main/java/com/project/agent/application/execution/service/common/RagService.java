package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.domain.execution.agent.exception.ConversationNotFoundInVectorStoreException;
import com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException;
import com.project.agent.domain.execution.agent.exception.UnsupportedEmbeddingModelException;
import com.project.agent.domain.execution.agent.exception.VectorSearchException;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.identity.ConversationId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides Retrieval-Augmented Generation (RAG) support for agent executions.
 *
 * <p>This service is responsible for retrieving relevant context from the
 * configured vector store before an LLM invocation and indexing assistant
 * responses after successful completion.
 *
 * <p>Vector retrieval and indexing are intentionally best-effort operations.
 * Failures during retrieval degrade gracefully so that agent execution can
 * continue without additional context. Configuration errors, such as an
 * unsupported embedding model, are surfaced to the caller.
 */
@Service
@RequiredArgsConstructor
public class RagService {

    // TODO Move to configuration properties.
    private static final int RAG_TOP_K = 5;

    private final VectorStorePort vectorStore;

    /**
     * Retrieves relevant passages from the vector store and converts them
     * into SYSTEM chat messages.
     */
    public List<ChatMessage> retrieveContext(
            ConversationId conversationId,
            String query
    ) {

        try {

            List<RetrievedPassage> passages =
                    vectorStore.search(
                            conversationId,
                            query,
                            RAG_TOP_K
                    );

            List<ChatMessage> context = new ArrayList<>();

            for (RetrievedPassage passage : passages) {

                context.add(
                        new ChatMessage(
                                MessageRole.SYSTEM,
                                passage.content()
                        )
                );
            }

            return context;

        } catch (ConversationNotFoundInVectorStoreException e) {

            // First execution. Nothing indexed yet.
            return List.of();

        } catch (VectorSearchException e) {

            // Retrieval is best effort.
            return List.of();
        }
    }

    /**
     * Indexes the assistant response so it becomes available for future
     * retrieval operations.
     */
    public void indexAssistantResponse(
            ConversationId conversationId,
            Message assistantMessage
    ) {

        try {

            vectorStore.index(
                    conversationId,
                    assistantMessage.getId(),
                    assistantMessage.getContent().value()
            );

        } catch (UnsupportedEmbeddingModelException e) {
            throw e;

        } catch (EmbeddingGenerationException e) {
        }
    }
}


