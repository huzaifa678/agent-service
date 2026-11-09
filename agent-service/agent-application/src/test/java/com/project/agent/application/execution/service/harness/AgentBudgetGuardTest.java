package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.exception.BudgetExceededException;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentBudgetGuardTest {

    private static final Currency USD = Currency.getInstance("USD");

    private final ConversationId conversationId = ConversationId.of(UUID.randomUUID());

    private Money usd(String amount) {
        return Money.of(new BigDecimal(amount), USD);
    }

    @Test
    void disabled_neverThrows_evenOverCeiling() {

        AgentBudgetGuard guard = new AgentBudgetGuard(false, 10, "0");

        guard.record(conversationId, TokenUsage.of(100, 100), usd("5.00"));

        assertThatCode(() -> guard.check(conversationId)).doesNotThrowAnyException();
    }

    @Test
    void unlimited_byDefault_whenCeilingsAreZero() {

        AgentBudgetGuard guard = new AgentBudgetGuard(true, 0, "0");

        guard.record(conversationId, TokenUsage.of(1_000, 1_000), usd("999.00"));

        assertThatCode(() -> guard.check(conversationId)).doesNotThrowAnyException();
    }

    @Test
    void allowsTurn_whenUnderTokenCeiling() {

        AgentBudgetGuard guard = new AgentBudgetGuard(true, 500, "0");

        guard.record(conversationId, TokenUsage.of(100, 100), usd("0.10"));

        assertThatCode(() -> guard.check(conversationId)).doesNotThrowAnyException();
    }

    @Test
    void refusesTurn_whenTokenCeilingReached() {

        AgentBudgetGuard guard = new AgentBudgetGuard(true, 200, "0");

        guard.record(conversationId, TokenUsage.of(150, 100), usd("0.10"));

        assertThatThrownBy(() -> guard.check(conversationId))
                .isInstanceOf(BudgetExceededException.class)
                .hasMessageContaining("token ceiling");
    }

    @Test
    void refusesTurn_whenCostCeilingReached() {

        AgentBudgetGuard guard = new AgentBudgetGuard(true, 0, "1.00");

        guard.record(conversationId, TokenUsage.of(10, 10), usd("1.50"));

        assertThatThrownBy(() -> guard.check(conversationId))
                .isInstanceOf(BudgetExceededException.class)
                .hasMessageContaining("cost ceiling");
    }

    @Test
    void budgetIsPerConversation() {

        AgentBudgetGuard guard = new AgentBudgetGuard(true, 100, "0");

        guard.record(conversationId, TokenUsage.of(100, 100), usd("0.10"));

        ConversationId other = ConversationId.of(UUID.randomUUID());
        assertThatCode(() -> guard.check(other)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check(conversationId))
                .isInstanceOf(BudgetExceededException.class);
    }
}
