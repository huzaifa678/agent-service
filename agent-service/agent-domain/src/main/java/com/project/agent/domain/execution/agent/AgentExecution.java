package com.project.agent.domain.execution.agent;

import com.project.agent.domain.execution.tool.ToolExecution;
import com.project.agent.domain.vo.billing.Money;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.ai.Latency;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.TokenUsage;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Records a single LLM invocation within a {@link com.project.agent.domain.conversation.Conversation}.
 * Created via {@link #start} when the agent begins processing; finalised by calling
 * {@link #complete}, {@link #fail}, or {@link #timeout}, each of which stamps
 * {@code completedAt} and computes {@link Latency}. Tool calls made during the
 * invocation are tracked as child {@link ToolExecution} entities.
 *
 * <p>State machine: {@code RUNNING → COMPLETED | FAILED | TIMEOUT}.
 * Any transition attempt from a non-{@code RUNNING} state throws
 * {@link IllegalStateException}.
 */
public class AgentExecution {

    private final AgentExecutionId id;

    private final ConversationId conversationId;

    private final ModelName modelName;

    private final ProviderName providerName;

    private AgentExecutionStatus status;

    private TokenUsage tokenUsage;

    private Money cost;

    private Latency latency;

    private final List<ToolExecution> toolExecutions;

    private final Instant startedAt;

    private Instant completedAt;

    private AgentExecution(
            AgentExecutionId id,
            ConversationId conversationId,
            ModelName modelName,
            ProviderName providerName
    ) {
        this.id = Objects.requireNonNull(id);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.modelName = Objects.requireNonNull(modelName);
        this.providerName = Objects.requireNonNull(providerName);

        this.status = AgentExecutionStatus.RUNNING;
        this.tokenUsage = TokenUsage.empty();
        this.cost = Money.zero(Currency.getInstance("USD"));
        this.latency = Latency.of(Duration.ZERO);
        this.toolExecutions = new ArrayList<>();
        this.startedAt = Instant.now();
    }

    /** Create a new execution in {@code RUNNING} state, stamped with the current time. */
    public static AgentExecution start(
            AgentExecutionId id,
            ConversationId conversationId,
            ModelName modelName,
            ProviderName providerName
    ) {
        return new AgentExecution(
                id,
                conversationId,
                modelName,
                providerName
        );
    }

    private AgentExecution(
            AgentExecutionId id,
            ConversationId conversationId,
            ModelName modelName,
            ProviderName providerName,
            AgentExecutionStatus status,
            TokenUsage tokenUsage,
            Money cost,
            Latency latency,
            List<ToolExecution> toolExecutions,
            Instant startedAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.conversationId = Objects.requireNonNull(conversationId);
        this.modelName = Objects.requireNonNull(modelName);
        this.providerName = Objects.requireNonNull(providerName);
        this.status = Objects.requireNonNull(status);
        this.tokenUsage = Objects.requireNonNull(tokenUsage);
        this.cost = Objects.requireNonNull(cost);
        this.latency = Objects.requireNonNull(latency);
        this.toolExecutions = new ArrayList<>(toolExecutions);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.completedAt = completedAt;
    }

    /** Rehydrate a persisted execution with its final state, metrics, and tool calls. Persistence-adapter use only. */
    public static AgentExecution reconstitute(
            AgentExecutionId id,
            ConversationId conversationId,
            ModelName modelName,
            ProviderName providerName,
            AgentExecutionStatus status,
            TokenUsage tokenUsage,
            Money cost,
            Latency latency,
            List<ToolExecution> toolExecutions,
            Instant startedAt,
            Instant completedAt
    ) {
        return new AgentExecution(
                id, conversationId, modelName, providerName, status,
                tokenUsage, cost, latency, toolExecutions, startedAt, completedAt);
    }

    /** Transition to {@code COMPLETED}, recording final token usage, cost, and latency. */
    public void complete(
            TokenUsage tokenUsage,
            Money cost
    ) {

        ensureRunning();

        this.tokenUsage = Objects.requireNonNull(tokenUsage);
        this.cost = Objects.requireNonNull(cost);

        this.completedAt = Instant.now();

        this.latency = Latency.of(
                Duration.between(startedAt, completedAt)
        );

        this.status = AgentExecutionStatus.COMPLETED;
    }

    /** Transition to {@code FAILED}, stamping latency from start to now. */
    public void fail() {

        ensureRunning();

        this.completedAt = Instant.now();

        this.latency = Latency.of(
                Duration.between(startedAt, completedAt)
        );

        this.status = AgentExecutionStatus.FAILED;
    }

    /** Transition to {@code TIMEOUT}, stamping latency from start to now. */
    public void timeout() {

        ensureRunning();

        this.completedAt = Instant.now();

        this.latency = Latency.of(
                Duration.between(startedAt, completedAt)
        );

        this.status = AgentExecutionStatus.TIMEOUT;
    }

    /** Attach a tool execution that was triggered during this agent execution. */
    public void addToolExecution(ToolExecution toolExecution) {

        Objects.requireNonNull(toolExecution);

        toolExecutions.add(toolExecution);
    }

    /** Returns the number of tool calls made during this execution. */
    public int totalToolExecutions() {
        return toolExecutions.size();
    }

    private void ensureRunning() {

        if (status != AgentExecutionStatus.RUNNING) {
            throw new IllegalStateException(
                    "Execution is no longer running."
            );
        }
    }

    public AgentExecutionId getId() {
        return id;
    }

    public ConversationId getConversationId() {
        return conversationId;
    }

    public ModelName getModelName() {
        return modelName;
    }

    public ProviderName getProviderName() {
        return providerName;
    }

    public AgentExecutionStatus getStatus() {
        return status;
    }

    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public Money getCost() {
        return cost;
    }

    public Latency getLatency() {
        return latency;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /** Returns an unmodifiable view of all tool executions triggered by this invocation. */
    public List<ToolExecution> getToolExecutions() {
        return Collections.unmodifiableList(toolExecutions);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AgentExecution other)) {
            return false;
        }

        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
