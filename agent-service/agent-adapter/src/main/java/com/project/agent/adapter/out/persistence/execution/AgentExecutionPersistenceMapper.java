package com.project.agent.adapter.out.persistence.execution;

import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.agent.AgentExecutionStatus;
import com.project.agent.domain.execution.tool.ToolExecution;
import com.project.agent.domain.execution.tool.ToolExecutionStatus;
import com.project.agent.domain.vo.ai.Latency;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.ToolName;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.ToolExecutionId;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

/** Converts between the {@code AgentExecution} aggregate and its JPA persistence model. */
@Component
public class AgentExecutionPersistenceMapper {

    public AgentExecutionJpaEntity toJpa(AgentExecution execution) {
        List<ToolExecutionJpaEntity> toolExecutions = execution.getToolExecutions().stream()
                .map(this::toJpa)
                .collect(Collectors.toCollection(ArrayList::new));

        return AgentExecutionJpaEntity.builder()
                .id(execution.getId().value())
                .conversationId(execution.getConversationId().value())
                .modelName(execution.getModelName().value())
                .providerName(execution.getProviderName().value())
                .status(execution.getStatus().name())
                .promptTokens(execution.getTokenUsage().promptTokens())
                .completionTokens(execution.getTokenUsage().completionTokens())
                .costAmount(execution.getCost().amount())
                .costCurrency(execution.getCost().currency().getCurrencyCode())
                .latencyMillis(execution.getLatency().toMillis())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .toolExecutions(toolExecutions)
                .build();
    }

    public AgentExecution toDomain(AgentExecutionJpaEntity entity) {
        List<ToolExecution> toolExecutions = entity.getToolExecutions().stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));

        return AgentExecution.reconstitute(
                AgentExecutionId.of(entity.getId()),
                ConversationId.of(entity.getConversationId()),
                ModelName.of(entity.getModelName()),
                ProviderName.of(entity.getProviderName()),
                AgentExecutionStatus.valueOf(entity.getStatus()),
                TokenUsage.of(entity.getPromptTokens(), entity.getCompletionTokens()),
                Money.of(entity.getCostAmount(), Currency.getInstance(entity.getCostCurrency())),
                Latency.of(Duration.ofMillis(entity.getLatencyMillis())),
                toolExecutions,
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }

    private ToolExecutionJpaEntity toJpa(ToolExecution tool) {
        return ToolExecutionJpaEntity.builder()
                .id(tool.getId().value())
                .toolName(tool.getToolName().value())
                .request(tool.getRequest().value())
                .response(tool.getResponse() == null ? null : tool.getResponse().value())
                .status(tool.getStatus().name())
                .latencyMillis(tool.getLatency().toMillis())
                .startedAt(tool.getStartedAt())
                .completedAt(tool.getCompletedAt())
                .build();
    }

    private ToolExecution toDomain(ToolExecutionJpaEntity entity) {
        return ToolExecution.reconstitute(
                ToolExecutionId.of(entity.getId()),
                ToolName.of(entity.getToolName()),
                MessageContent.of(entity.getRequest()),
                entity.getResponse() == null ? null : MessageContent.of(entity.getResponse()),
                ToolExecutionStatus.valueOf(entity.getStatus()),
                Latency.of(Duration.ofMillis(entity.getLatencyMillis())),
                entity.getStartedAt(),
                entity.getCompletedAt()
        );
    }
}
