package com.project.agent.application.execution.port.out.rag;

import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;

import java.util.List;

/**
 * Outbound port for the pgvector-backed RAG store (implemented via langchain4j-pgvector).
 *
 * <p>{@link #index} may raise
 * {@link com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException} or
 * {@link com.project.agent.domain.execution.agent.exception.UnsupportedEmbeddingModelException};
 * {@link #search} may raise
 * {@link com.project.agent.domain.execution.agent.exception.VectorSearchException} or
 * {@link com.project.agent.domain.execution.agent.exception.ConversationNotFoundInVectorStoreException}.
 */
public interface VectorStorePort {

    /** Embed and store a message's content for later retrieval. */
    void index(ConversationId conversationId, MessageId messageId, String content);

    /** Return up to {@code topK} passages most similar to {@code query} within the conversation. */
    List<RetrievedPassage> search(ConversationId conversationId, String query, int topK);
}
