package com.project.agent.adapter.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Jackson 2 {@link ObjectMapper} for the conversation event store (jsonb payloads).
 *
 * <p>Spring Boot 4 autoconfigures Jackson 3 ({@code tools.jackson}) and no longer exposes a
 * {@code com.fasterxml.jackson.databind.ObjectMapper} bean, but {@code ConversationEventSerializer}
 * is built on Jackson 2. Define it explicitly, registering every Jackson 2 {@link Module} bean plus
 * JSR-310 (events carry {@code Instant}/{@code OffsetDateTime}) and emitting ISO-8601 timestamps.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(List<Module> modules) {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModules(modules)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
