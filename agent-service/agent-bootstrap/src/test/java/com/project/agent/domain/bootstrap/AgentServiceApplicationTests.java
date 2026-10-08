package com.project.agent.domain.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test: the full application context boots. It needs real infrastructure, so it
 * runs against singleton Testcontainers (pgvector Postgres + Kafka) with the {@code test}
 * profile supplying dummy LLM keys and disabling the OTLP exporters / cloud verifier —
 * the same harness the adapter integration tests use.
 */
@SpringBootTest
@ActiveProfiles("test")
class AgentServiceApplicationTests {

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg17")
                    .withDatabaseName("agent")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    static {
        postgres.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("agent.vector.host", postgres::getHost);
        registry.add("agent.vector.port", postgres::getFirstMappedPort);
        registry.add("agent.vector.database", postgres::getDatabaseName);
        registry.add("agent.vector.user", postgres::getUsername);
        registry.add("agent.vector.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Test
    void contextLoads() {
    }
}
