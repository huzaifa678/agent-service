package com.project.agent.adapter.out.vector;

import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException;
import com.project.agent.domain.execution.agent.exception.VectorSearchException;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * pgvector-backed {@link VectorStorePort} via langchain4j. Embeds message content
 * with the configured embedding model and stores/searches it in Postgres, scoped
 * per conversation through metadata filtering. Embedding/search failures are
 * surfaced as the corresponding domain exceptions; the orchestrator treats RAG as
 * best-effort.
 */
@Component
public class PgVectorStoreAdapter implements VectorStorePort {

    private static final String CONVERSATION_KEY = "conversationId";
    private static final String MESSAGE_KEY = "messageId";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public PgVectorStoreAdapter(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Override
    public void index(ConversationId conversationId, MessageId messageId, String content) {
        try {
            Embedding embedding = embeddingModel.embed(content).content();
            Metadata metadata = new Metadata();
            metadata.put(CONVERSATION_KEY, conversationId.toString());
            metadata.put(MESSAGE_KEY, messageId.toString());
            embeddingStore.add(embedding, TextSegment.from(content, metadata));
        } catch (RuntimeException e) {
            throw new EmbeddingGenerationException(
                    "Failed to embed/index message " + messageId + " for conversation " + conversationId, e);
        }
    }

    @Override
    public List<RetrievedPassage> search(ConversationId conversationId, String query, int topK) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            Filter filter = MetadataFilterBuilder.metadataKey(CONVERSATION_KEY)
                    .isEqualTo(conversationId.toString());
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(topK)
                    .filter(filter)
                    .build();

            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            List<RetrievedPassage> passages = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : result.matches()) {
                passages.add(new RetrievedPassage(match.embedded().text(), match.score()));
            }
            return passages;
        } catch (RuntimeException e) {
            throw new VectorSearchException(
                    "Vector search failed for conversation " + conversationId, e);
        }
    }
}
