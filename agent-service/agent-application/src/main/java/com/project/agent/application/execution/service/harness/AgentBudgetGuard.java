package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.exception.BudgetExceededException;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.identity.ConversationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-subject token and cost budget for agent executions — the harness's cost-governance
 * boundary, alongside memory, tools, and I/O.
 *
 * <p>The {@link MemoryRateLimiter} caps the <em>frequency</em> of memory operations; this caps
 * cumulative <em>spend</em>. Each completed turn's token usage and cost are accumulated against
 * a subject, and a turn is refused once the subject is over either ceiling. It is a hard stop
 * (a {@link BudgetExceededException}, surfaced as HTTP 429), not best-effort degradation: the
 * point is to stop spending.
 *
 * <p>The subject is the conversation today. {@code RunAgentCommand} does not yet carry a
 * {@code TenantId}; once it does, keying this guard by tenant turns it into the per-tenant
 * monthly budget it is really meant to be — the enforcement here does not change, only the
 * key. Counters are in process and never evicted, matching {@link MemoryRateLimiter}: fine for
 * the request volumes here and for a single replica, but not shared across replicas and reset
 * on restart. A distributed counter (Redis) is the natural next step for multi-replica
 * fairness and durability.
 *
 * <p>Disabled by default ({@code agent.budget.enabled=false}) and a no-op when both ceilings
 * are left at {@code 0} (unlimited), so wiring it in changes no existing behaviour.
 */
@Component
public class AgentBudgetGuard {

    private static final Logger log = LoggerFactory.getLogger(AgentBudgetGuard.class);

    private final boolean enabled;
    private final long maxTokens;
    private final BigDecimal maxCostUsd;

    private final ConcurrentMap<String, Spend> spend = new ConcurrentHashMap<>();

    public AgentBudgetGuard(
            @Value("${agent.budget.enabled:false}") boolean enabled,
            @Value("${agent.budget.max-tokens:0}") long maxTokens,
            @Value("${agent.budget.max-cost-usd:0}") String maxCostUsd
    ) {
        this.enabled = enabled;
        this.maxTokens = maxTokens;
        this.maxCostUsd = new BigDecimal(maxCostUsd);
    }

    /**
     * Refuse the turn if the conversation has already exhausted its budget.
     *
     * @throws BudgetExceededException if an enabled ceiling has been reached
     */
    public void check(ConversationId conversationId) {

        if (!enabled) {
            return;
        }

        Spend current = spend.get(conversationId.toString());
        if (current == null) {
            return;
        }

        if (maxTokens > 0 && current.tokens() >= maxTokens) {
            log.info(
                    "Budget: conversation {} reached the token ceiling ({} >= {}).",
                    conversationId, current.tokens(), maxTokens
            );
            throw new BudgetExceededException(
                    "token ceiling of " + maxTokens + " reached for this conversation");
        }

        if (isCostCapped() && current.cost().compareTo(maxCostUsd) >= 0) {
            log.info(
                    "Budget: conversation {} reached the cost ceiling ({} >= {} USD).",
                    conversationId, current.cost().toPlainString(), maxCostUsd.toPlainString()
            );
            throw new BudgetExceededException(
                    "cost ceiling of " + maxCostUsd.toPlainString() + " USD reached for this conversation");
        }
    }

    /**
     * Accumulate a completed turn's usage against the conversation's budget.
     */
    public void record(ConversationId conversationId, TokenUsage tokenUsage, Money cost) {

        if (!enabled) {
            return;
        }

        spend.computeIfAbsent(conversationId.toString(), key -> new Spend())
                .add(tokenUsage.totalTokens(), cost.amount());
    }

    private boolean isCostCapped() {
        return maxCostUsd.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Mutable per-subject accumulator; guarded by its own monitor. */
    private static final class Spend {

        private long tokens;
        private BigDecimal cost = BigDecimal.ZERO;

        synchronized void add(long addedTokens, BigDecimal addedCost) {
            this.tokens += addedTokens;
            this.cost = this.cost.add(addedCost);
        }

        synchronized long tokens() {
            return tokens;
        }

        synchronized BigDecimal cost() {
            return cost;
        }
    }
}
