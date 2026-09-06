package com.project.agent.adapter.out.vector;

import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException;
import com.project.agent.domain.execution.agent.exception.VectorSearchException;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid {@link VectorStorePort}: fuses dense (vector) and sparse (Postgres full-text)
 * retrieval so exact tokens a customer types — invoice IDs, error codes, plan names —
 * are found even when the embedding does not rank them highly, while semantically
 * similar passages are still recalled.
 *
 * <ul>
 *   <li><b>Dense</b> — langchain4j similarity search over the pgvector index.</li>
 *   <li><b>Sparse</b> — {@code websearch_to_tsquery} full-text match on the passage text,
 *       scoped to the conversation, with the cosine similarity computed in the same query
 *       (the exact langchain4j formula {@code (2 - (embedding <=> q)) / 2}) so every
 *       candidate carries a comparable 0–1 score for downstream gating.</li>
 *   <li><b>Fusion</b> — Reciprocal Rank Fusion (RRF) over the two ranked lists decides the
 *       final order; each surviving passage keeps its cosine score.</li>
 * </ul>
 *
 * <p>Marked {@link Primary} so it is the {@link VectorStorePort} the application uses;
 * {@link PgVectorStoreAdapter} remains for indexing and as the dense building block.
 * Sparse retrieval is best-effort: if it fails (or is disabled) the adapter degrades to
 * dense-only. Indexing is delegated unchanged.
 */
@Component
@Primary
public class HybridVectorStoreAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(HybridVectorStoreAdapter.class);

    private static final String CONVERSATION_KEY = "conversationId";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final JdbcTemplate jdbcTemplate;
    private final PgVectorStoreAdapter indexDelegate;

    private final boolean hybridEnabled;
    private final int denseLimit;
    private final int sparseLimit;
    private final int rrfK;
    private final String table;

    public HybridVectorStoreAdapter(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            JdbcTemplate jdbcTemplate,
            PgVectorStoreAdapter indexDelegate,
            @Value("${agent.rag.hybrid.enabled:true}") boolean hybridEnabled,
            @Value("${agent.rag.hybrid.dense-limit:20}") int denseLimit,
            @Value("${agent.rag.hybrid.sparse-limit:20}") int sparseLimit,
            @Value("${agent.rag.hybrid.rrf-k:60}") int rrfK,
            @Value("${agent.vector.table:conversation_embeddings}") String table
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.jdbcTemplate = jdbcTemplate;
        this.indexDelegate = indexDelegate;
        this.hybridEnabled = hybridEnabled;
        this.denseLimit = denseLimit;
        this.sparseLimit = sparseLimit;
        this.rrfK = rrfK;
        this.table = table;
    }

    @Override
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "indexFallback")
    public void index(ConversationId conversationId, MessageId messageId, String content) {
        indexDelegate.index(conversationId, messageId, content);
    }

    @Override
    @CircuitBreaker(name = "vectorStore", fallbackMethod = "searchFallback")
    public List<RetrievedPassage> search(ConversationId conversationId, String query, int topK) {

        Embedding queryEmbedding = embed(query, conversationId);

        List<RetrievedPassage> dense =
                denseSearch(queryEmbedding, conversationId);

        List<RetrievedPassage> sparse =
                hybridEnabled
                        ? sparseSearch(queryEmbedding, conversationId, query)
                        : List.of();

        return fuse(dense, sparse, topK);
    }

    /**
     * Resilience4j fallback for {@link #search}. When the {@code vectorStore} circuit is open
     * the long-term store is treated as briefly unavailable: the failure is surfaced as a
     * {@link VectorSearchException}, which {@code RagService} already handles best-effort by
     * degrading to the short-term window plus a grounding guard rather than failing the turn.
     * Domain exceptions are passed through unchanged.
     */
    @SuppressWarnings("unused")
    private List<RetrievedPassage> searchFallback(
            ConversationId conversationId, String query, int topK, Throwable t) {
        if (t instanceof VectorSearchException vse) {
            throw vse;
        }
        throw new VectorSearchException(
                "Long-term memory circuit open for conversation " + conversationId, t);
    }

    /**
     * Resilience4j fallback for {@link #index}. A genuine configuration error
     * ({@code UnsupportedEmbeddingModelException}) is propagated so it surfaces; every other
     * failure — including an open circuit — becomes a best-effort
     * {@link EmbeddingGenerationException}, which {@code RagService} swallows (a memory write
     * must never fail the turn).
     */
    @SuppressWarnings("unused")
    private void indexFallback(
            ConversationId conversationId, MessageId messageId, String content, Throwable t) {
        if (t instanceof com.project.agent.domain.execution.agent.exception.UnsupportedEmbeddingModelException unsupported) {
            throw unsupported;
        }
        if (t instanceof EmbeddingGenerationException ege) {
            throw ege;
        }
        throw new EmbeddingGenerationException(
                "Long-term memory circuit open for conversation " + conversationId, t);
    }

    private Embedding embed(String query, ConversationId conversationId) {
        try {
            return embeddingModel.embed(query).content();
        } catch (RuntimeException e) {
            throw new VectorSearchException(
                    "Failed to embed query for conversation " + conversationId, e);
        }
    }

    private List<RetrievedPassage> denseSearch(
            Embedding queryEmbedding,
            ConversationId conversationId
    ) {
        try {
            Filter filter = MetadataFilterBuilder.metadataKey(CONVERSATION_KEY)
                    .isEqualTo(conversationId.toString());

            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(denseLimit)
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
                    "Dense vector search failed for conversation " + conversationId, e);
        }
    }

    /**
     * Sparse full-text retrieval. Returns each match's text with the same cosine-based
     * similarity langchain4j reports, so scores are directly comparable to the dense
     * side. Best-effort: any failure degrades the request to dense-only.
     */
    private List<RetrievedPassage> sparseSearch(
            Embedding queryEmbedding,
            ConversationId conversationId,
            String query
    ) {

        String sql = String.format(
                "SELECT text, (2 - (embedding <=> CAST(? AS vector))) / 2 AS cosine "
                        + "FROM %s "
                        + "WHERE metadata ->> '" + CONVERSATION_KEY + "' = ? "
                        + "  AND to_tsvector('english', text) "
                        + "      @@ websearch_to_tsquery('english', ?) "
                        + "ORDER BY ts_rank_cd("
                        + "      to_tsvector('english', text), "
                        + "      websearch_to_tsquery('english', ?)) DESC "
                        + "LIMIT ?",
                table
        );

        String vectorLiteral = toVectorLiteral(queryEmbedding);

        try {
            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new RetrievedPassage(
                            rs.getString("text"),
                            rs.getDouble("cosine")
                    ),
                    vectorLiteral,
                    conversationId.toString(),
                    query,
                    query,
                    sparseLimit
            );
        } catch (RuntimeException e) {
            log.warn(
                    "Sparse full-text retrieval failed for conversation {}; "
                            + "degrading to dense-only.",
                    conversationId,
                    e
            );
            return List.of();
        }
    }

    /**
     * Reciprocal Rank Fusion: each passage scores {@code sum(1 / (rrfK + rank))} across the
     * lists it appears in. The result is ordered by fused score; each passage keeps the
     * strongest cosine score seen for it, which downstream gating thresholds against.
     */
    private List<RetrievedPassage> fuse(
            List<RetrievedPassage> dense,
            List<RetrievedPassage> sparse,
            int topK
    ) {

        Map<String, Double> fusedScore = new LinkedHashMap<>();
        Map<String, Double> cosineByText = new LinkedHashMap<>();

        accumulate(dense, fusedScore, cosineByText);
        accumulate(sparse, fusedScore, cosineByText);

        return fusedScore.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> new RetrievedPassage(
                        entry.getKey(),
                        cosineByText.get(entry.getKey())
                ))
                .toList();
    }

    private void accumulate(
            List<RetrievedPassage> ranked,
            Map<String, Double> fusedScore,
            Map<String, Double> cosineByText
    ) {
        for (int rank = 0; rank < ranked.size(); rank++) {
            RetrievedPassage passage = ranked.get(rank);
            String text = passage.content();

            fusedScore.merge(text, 1.0 / (rrfK + rank), Double::sum);
            cosineByText.merge(text, passage.score(), Math::max);
        }
    }

    private String toVectorLiteral(Embedding embedding) {
        float[] vector = embedding.vector();
        StringBuilder builder = new StringBuilder(vector.length * 8);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}
