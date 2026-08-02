package com.project.agent.adapter.out.persistence.execution;

import com.project.agent.adapter.out.persistence.execution.AgentExecutionPersistenceAdapter;
import com.project.agent.adapter.support.PostgreSQLContainerConfig;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.agent.AgentExecutionStatus;
import com.project.agent.domain.execution.tool.ToolExecution;
import com.project.agent.domain.vo.ai.Latency;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.ai.ToolName;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.ToolExecutionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
class AgentExecutionPersistenceAdapterIT extends PostgreSQLContainerConfig {

    @Autowired
    private AgentExecutionPersistenceAdapter adapter;

    @Autowired
    private AgentExecutionJpaRepository repository;

    @Test
    void save_persistsExecutionWithToolExecutions() {
        AgentExecution execution = AgentExecution.start(
                AgentExecutionId.of(UUID.randomUUID()),
                ConversationId.of(UUID.randomUUID()),
                ModelName.of("gpt-4o"),
                ProviderName.of("openai")
        );
        execution.complete(
                TokenUsage.of(100, 50),
                Money.of(new BigDecimal("0.0020"), Currency.getInstance("USD"))
        );
        execution.addToolExecution(ToolExecution.start(
                ToolExecutionId.of(UUID.randomUUID()),
                ToolName.of("calculator"),
                MessageContent.of("2+2")
        ));

        AgentExecution saved = adapter.save(execution);

        assertThat(saved.getId()).isEqualTo(execution.getId());
        assertThat(saved.getStatus()).isEqualTo(AgentExecutionStatus.COMPLETED);
        assertThat(saved.getTokenUsage().promptTokens()).isEqualTo(100);
        assertThat(saved.getTokenUsage().completionTokens()).isEqualTo(50);
        assertThat(saved.getCost().amount()).isEqualByComparingTo(new BigDecimal("0.0020"));
        assertThat(saved.getToolExecutions()).hasSize(1);
        assertThat(saved.getToolExecutions().getFirst().getToolName()).isEqualTo(ToolName.of("calculator"));
    }

    @Test
    void findById_existingExecution_returnsExecution() {
        AgentExecution execution = AgentExecution.start(
                AgentExecutionId.of(UUID.randomUUID()),
                ConversationId.of(UUID.randomUUID()),
                ModelName.of("gpt-4o"),
                ProviderName.of("openai")
        );
        execution.complete(
                TokenUsage.of(50, 25),
                Money.of(new BigDecimal("0.0010"), Currency.getInstance("USD"))
        );
        adapter.save(execution);

        Optional<AgentExecution> found = adapter.findById(execution.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(AgentExecutionStatus.COMPLETED);
        assertThat(found.get().getTokenUsage().promptTokens()).isEqualTo(50);
    }

    @Test
    void findById_nonExistingExecution_returnsEmpty() {
        Optional<AgentExecution> found = adapter.findById(AgentExecutionId.of(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }

    @Test
    void findByConversationId_returnsExecutionsForConversation() {
        UUID conversationId = UUID.randomUUID();

        AgentExecution exec1 = AgentExecution.start(
                AgentExecutionId.of(UUID.randomUUID()),
                ConversationId.of(conversationId),
                ModelName.of("gpt-4o"),
                ProviderName.of("openai")
        );
        exec1.complete(
                TokenUsage.of(100, 50),
                Money.of(new BigDecimal("0.0020"), Currency.getInstance("USD"))
        );

        AgentExecution exec2 = AgentExecution.start(
                AgentExecutionId.of(UUID.randomUUID()),
                ConversationId.of(conversationId),
                ModelName.of("claude-3-5-sonnet"),
                ProviderName.of("anthropic")
        );
        exec2.complete(
                TokenUsage.of(80, 40),
                Money.of(new BigDecimal("0.0030"), Currency.getInstance("USD"))
        );

        adapter.save(exec1);
        adapter.save(exec2);

        List<AgentExecution> results = adapter.findByConversationId(ConversationId.of(conversationId));

        assertThat(results).hasSize(2);
        assertThat(results).extracting("id")
                .containsExactlyInAnyOrder(exec1.getId(), exec2.getId());
    }
}
