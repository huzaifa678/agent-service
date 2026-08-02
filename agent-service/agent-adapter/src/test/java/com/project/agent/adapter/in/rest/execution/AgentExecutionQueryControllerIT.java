package com.project.agent.adapter.in.rest.execution;

import com.project.agent.adapter.out.persistence.execution.AgentExecutionJpaRepository;
import com.project.agent.adapter.out.persistence.execution.AgentExecutionJpaEntity;
import com.project.agent.adapter.support.PostgreSQLContainerConfig;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.agent.AgentExecutionStatus;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Testcontainers
class AgentExecutionQueryControllerIT extends PostgreSQLContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentExecutionJpaRepository agentExecutionJpaRepository;

    private String conversationId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID().toString();
    }

    @Test
    void getById_existingExecution_returns200() throws Exception {
        AgentExecutionJpaEntity entity = seedExecution(
                conversationId, "gpt-4o", "openai", AgentExecutionStatus.COMPLETED
        );

        mockMvc.perform(get("/api/agent/executions/{id}", entity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entity.getId().toString()))
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andExpect(jsonPath("$.modelName").value("gpt-4o"))
                .andExpect(jsonPath("$.providerName").value("openai"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.promptTokens").value(100))
                .andExpect(jsonPath("$.completionTokens").value(50))
                .andExpect(jsonPath("$.totalTokens").value(150))
                .andExpect(jsonPath("$.costAmount").value(0.0020))
                .andExpect(jsonPath("$.costCurrency").value("USD"))
                .andExpect(jsonPath("$.latencyMillis").value(1500));
    }

    @Test
    void getById_nonExistingExecution_returns404() throws Exception {
        mockMvc.perform(get("/api/agent/executions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AgentExecutionNotFoundException"));
    }

    @Test
    void byConversation_returnsExecutionsForConversation() throws Exception {
        seedExecution(conversationId, "gpt-4o", "openai", AgentExecutionStatus.COMPLETED);
        seedExecution(conversationId, "claude-3-5-sonnet", "anthropic", AgentExecutionStatus.FAILED);

        mockMvc.perform(get("/api/agent/executions")
                        .param("conversationId", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].conversationId").value(conversationId))
                .andExpect(jsonPath("$[1].conversationId").value(conversationId));
    }

    @Test
    void byConversation_nonExistingConversation_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/agent/executions")
                        .param("conversationId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private AgentExecutionJpaEntity seedExecution(
            String conversationId,
            String modelName,
            String providerName,
            AgentExecutionStatus status
    ) {
        AgentExecution domain = AgentExecution.start(
                AgentExecutionId.of(UUID.randomUUID()),
                ConversationId.of(UUID.fromString(conversationId)),
                ModelName.of(modelName),
                ProviderName.of(providerName)
        );

        if (status == AgentExecutionStatus.COMPLETED) {
            domain.complete(
                    TokenUsage.of(100, 50),
                    Money.of(new BigDecimal("0.0020"), Currency.getInstance("USD"))
            );
        } else if (status == AgentExecutionStatus.FAILED) {
            domain.fail();
        }

        AgentExecutionJpaEntity entity = AgentExecutionJpaEntity.builder()
                .id(domain.getId().value())
                .conversationId(domain.getConversationId().value())
                .modelName(domain.getModelName().value())
                .providerName(domain.getProviderName().value())
                .status(domain.getStatus().name())
                .promptTokens(domain.getTokenUsage().promptTokens())
                .completionTokens(domain.getTokenUsage().completionTokens())
                .costAmount(domain.getCost().amount())
                .costCurrency(domain.getCost().currency().getCurrencyCode())
                .latencyMillis(domain.getLatency().toMillis())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt() != null ? domain.getCompletedAt() : Instant.now())
                .build();

        return agentExecutionJpaRepository.save(entity);
    }
}
