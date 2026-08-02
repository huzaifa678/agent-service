package com.project.agent.adapter.out.persistence.execution;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** JPA persistence model for an {@code AgentExecution} and its tool calls. */
@Entity
@Table(name = "agent_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentExecutionJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(length = 128, nullable = false)
    private String modelName;

    @Column(length = 64, nullable = false)
    private String providerName;

    @Column(length = 16, nullable = false)
    private String status;

    private int promptTokens;

    private int completionTokens;

    @Column(precision = 19, scale = 4, nullable = false)
    private BigDecimal costAmount;

    @Column(length = 3, nullable = false)
    private String costCurrency;

    private long latencyMillis;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "agent_execution_id")
    @OrderColumn(name = "position")
    @Builder.Default
    private List<ToolExecutionJpaEntity> toolExecutions = new ArrayList<>();
}
