package com.project.agent.domain.execution.agent;

import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentExecutionTest {

    private static final Currency USD = Currency.getInstance("USD");

    private static AgentExecution running() {
        return AgentExecution.start(
                AgentExecutionId.of(UUID.randomUUID()),
                ConversationId.of(UUID.randomUUID()),
                ModelName.of("gpt-4o"),
                ProviderName.of("openai")
        );
    }

    @Test
    void startsInRunningState() {
        AgentExecution execution = running();
        assertEquals(AgentExecutionStatus.RUNNING, execution.getStatus());
        assertNotNull(execution.getStartedAt());
    }

    @Test
    void completeRecordsUsageCostAndLatency() {
        AgentExecution execution = running();
        execution.complete(TokenUsage.of(10, 5), Money.of("0.03", USD));

        assertEquals(AgentExecutionStatus.COMPLETED, execution.getStatus());
        assertEquals(15, execution.getTokenUsage().totalTokens());
        assertEquals(0, execution.getCost().amount().compareTo(new java.math.BigDecimal("0.03")));
        assertNotNull(execution.getCompletedAt());
    }

    @Test
    void failAndTimeoutAreTerminal() {
        AgentExecution failed = running();
        failed.fail();
        assertEquals(AgentExecutionStatus.FAILED, failed.getStatus());

        AgentExecution timedOut = running();
        timedOut.timeout();
        assertEquals(AgentExecutionStatus.TIMEOUT, timedOut.getStatus());
    }

    @Test
    void cannotTransitionOnceCompleted() {
        AgentExecution execution = running();
        execution.complete(TokenUsage.empty(), Money.zero(USD));
        assertThrows(IllegalStateException.class,
                () -> execution.complete(TokenUsage.empty(), Money.zero(USD)));
        assertThrows(IllegalStateException.class, execution::fail);
    }
}
