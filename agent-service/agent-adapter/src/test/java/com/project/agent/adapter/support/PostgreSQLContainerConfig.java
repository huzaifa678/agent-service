package com.project.agent.adapter.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public abstract class PostgreSQLContainerConfig {

    // Singleton container: started once per JVM and reused by every IT class, left
    // for Ryuk to reap at JVM exit. It is deliberately NOT a @Container under
    // @Testcontainers — that stops the static container after the first test class,
    // so the remaining classes sharing this JVM would fail with "Connection refused".
    // pgvector image (not plain postgres): the langchain4j PgVectorEmbeddingStore
    // bean runs `CREATE EXTENSION vector` on init, which a stock postgres lacks.
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg17")
                    .withDatabaseName("agent")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void postgresProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        // Hibernate auto-ddl is disabled in production, but enabled for ITs so the
        // schema is created/dropped in the container. This is a no-op for the
        // langchain4j PgVectorEmbeddingStore, which uses its own schema-init code.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // The langchain4j PgVectorEmbeddingStore connects via agent.vector.* (not
        // spring.datasource.*), which otherwise still point at the dev DB on
        // localhost:5434. Point them at this container so the store can init
        // (CREATE EXTENSION vector + createTable) against the pgvector image.
        registry.add("agent.vector.host", postgres::getHost);
        registry.add("agent.vector.port", postgres::getFirstMappedPort);
        registry.add("agent.vector.database", postgres::getDatabaseName);
        registry.add("agent.vector.user", postgres::getUsername);
        registry.add("agent.vector.password", postgres::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    // The container is shared across every IT class (singleton), so data written by
    // one test would otherwise leak into the next. Truncate all tables before each
    // test for a clean slate. Runs before any subclass @BeforeEach (superclass first),
    // so per-test seeding still applies.
    @BeforeEach
    void truncateAllTables() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            StringBuilder tables = new StringBuilder();
            try (ResultSet rs = statement.executeQuery(
                    "SELECT tablename FROM pg_tables WHERE schemaname = 'public'")) {
                while (rs.next()) {
                    if (tables.length() > 0) {
                        tables.append(", ");
                    }
                    tables.append('"').append(rs.getString(1)).append('"');
                }
            }
            if (tables.length() > 0) {
                statement.execute("TRUNCATE TABLE " + tables + " RESTART IDENTITY CASCADE");
            }
        }
    }
}
