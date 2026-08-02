package com.project.agent.adapter.out.llm.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;

/**
 * Wires the langchain4j clients. The two chat models back the primary→secondary
 * failover in {@link com.project.agent.adapter.out.llm.ChatModelAdapter}.
 *
 * <p>All beans are {@code @Lazy}: they are created on first use, so the Spring
 * context can start (and lightweight tests can load) without live provider keys
 * or a reachable pgvector database.
 */
@Configuration
public class LangChain4jConfig {

    @Bean("primaryChatModel")
    @Lazy
    public ChatModel primaryChatModel(
            @Value("${langchain4j.open-ai.api-key:}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model:gpt-4o}") String modelName
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(0) // Resilience4j owns retries; disable the client's own.
                .build();
    }

    @Bean("secondaryChatModel")
    @Lazy
    public ChatModel secondaryChatModel(
            @Value("${langchain4j.anthropic.api-key:}") String apiKey,
            @Value("${langchain4j.anthropic.chat-model:claude-3-5-sonnet-20241022}") String modelName
    ) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(1024)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(0)
                .build();
    }

    @Bean
    @Lazy
    public EmbeddingModel embeddingModel(
            @Value("${langchain4j.open-ai.api-key:}") String apiKey,
            @Value("${langchain4j.open-ai.embedding-model:text-embedding-3-small}") String modelName,
            @Value("${agent.vector.dimension:1536}") int dimension
    ) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .dimensions(dimension)
                .build();
    }

    @Bean
    @Lazy
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${agent.vector.host:localhost}") String host,
            @Value("${agent.vector.port:5434}") int port,
            @Value("${agent.vector.database:agent_db}") String database,
            @Value("${agent.vector.user:agent_user}") String user,
            @Value("${agent.vector.password:agent_password}") String password,
            @Value("${agent.vector.table:conversation_embeddings}") String table,
            @Value("${agent.vector.dimension:1536}") int dimension
    ) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(table)
                .dimension(dimension)
                .createTable(true)
                .build();
    }
}
