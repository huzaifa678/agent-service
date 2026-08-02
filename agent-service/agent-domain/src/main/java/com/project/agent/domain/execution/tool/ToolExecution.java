package com.project.agent.domain.execution.tool;

import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.ai.Latency;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ToolExecutionId;
import com.project.agent.domain.vo.ai.ToolName;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Records a single tool/function call made during an {@link AgentExecution}.
 * Created via {@link #start} when the agent dispatches the call; finalised by
 * {@link #complete} (with the tool's response) or {@link #fail}.
 * Latency is computed automatically from {@code startedAt} to {@code completedAt}.
 *
 * <p>State machine: {@code RUNNING → COMPLETED | FAILED}.
 */
public class ToolExecution {

    private final ToolExecutionId id;

    private final ToolName toolName;

    private final MessageContent request;

    private MessageContent response;

    private ToolExecutionStatus status;

    private Latency latency;

    private final Instant startedAt;

    private Instant completedAt;

    private ToolExecution(
            ToolExecutionId id,
            ToolName toolName,
            MessageContent request
    ) {
        this.id = Objects.requireNonNull(id);
        this.toolName = Objects.requireNonNull(toolName);
        this.request = Objects.requireNonNull(request);

        this.status = ToolExecutionStatus.RUNNING;
        this.latency = Latency.of(Duration.ZERO);
        this.startedAt = Instant.now();
    }

    /** Create a new tool execution in {@code RUNNING} state for the given tool and request payload. */
    public static ToolExecution start(
            ToolExecutionId id,
            ToolName toolName,
            MessageContent request
    ) {
        return new ToolExecution(
                id,
                toolName,
                request
        );
    }

    private ToolExecution(
            ToolExecutionId id,
            ToolName toolName,
            MessageContent request,
            MessageContent response,
            ToolExecutionStatus status,
            Latency latency,
            Instant startedAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.toolName = Objects.requireNonNull(toolName);
        this.request = Objects.requireNonNull(request);
        this.response = response;
        this.status = Objects.requireNonNull(status);
        this.latency = Objects.requireNonNull(latency);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.completedAt = completedAt;
    }

    /** Rehydrate a persisted tool execution. {@code response} may be {@code null} for a failed call. Persistence-adapter use only. */
    public static ToolExecution reconstitute(
            ToolExecutionId id,
            ToolName toolName,
            MessageContent request,
            MessageContent response,
            ToolExecutionStatus status,
            Latency latency,
            Instant startedAt,
            Instant completedAt
    ) {
        return new ToolExecution(id, toolName, request, response, status, latency, startedAt, completedAt);
    }

    /** Transition to {@code COMPLETED}, recording the tool's response and latency. */
    public void complete(MessageContent response) {

        ensureRunning();

        this.response = Objects.requireNonNull(response);

        this.completedAt = Instant.now();

        this.latency = Latency.of(
                Duration.between(startedAt, completedAt)
        );

        this.status = ToolExecutionStatus.COMPLETED;
    }

    /** Transition to {@code FAILED}, stamping latency from start to now. */
    public void fail() {

        ensureRunning();

        this.completedAt = Instant.now();

        this.latency = Latency.of(
                Duration.between(startedAt, completedAt)
        );

        this.status = ToolExecutionStatus.FAILED;
    }

    private void ensureRunning() {

        if (status != ToolExecutionStatus.RUNNING) {
            throw new IllegalStateException(
                    "Tool execution is already completed."
            );
        }
    }

    public ToolExecutionId getId() {
        return id;
    }

    public ToolName getToolName() {
        return toolName;
    }

    public MessageContent getRequest() {
        return request;
    }

    public MessageContent getResponse() {
        return response;
    }

    public ToolExecutionStatus getStatus() {
        return status;
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

    public boolean isRunning() {
        return status == ToolExecutionStatus.RUNNING;
    }

    public boolean isCompleted() {
        return status == ToolExecutionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ToolExecutionStatus.FAILED;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ToolExecution other)) {
            return false;
        }

        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
