package com.project.agent.adapter.out.persistence.execution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** JPA persistence model for a {@code ToolExecution} (child of an agent execution). */
@Entity
@Table(name = "tool_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolExecutionJpaEntity {

    @Id
    private UUID id;

    @Column(length = 128, nullable = false)
    private String toolName;

    @Column(columnDefinition = "text", nullable = false)
    private String request;

    @Column(columnDefinition = "text")
    private String response;

    @Column(length = 16, nullable = false)
    private String status;

    private long latencyMillis;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;
}
