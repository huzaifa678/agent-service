package com.project.agent.domain.vo.identity;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying a {@link com.project.agent.domain.execution.tool.ToolExecution}. */
public record ToolExecutionId(UUID value) {

    public ToolExecutionId {
        Objects.requireNonNull(value, "tool execution id must not be null");
    }

    public static ToolExecutionId of(UUID value) {
        return new ToolExecutionId(value);
    }

    public static ToolExecutionId of(String value) {
        return new ToolExecutionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
