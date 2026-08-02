package com.project.agent.domain.vo.identity;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying an {@link com.project.agent.domain.execution.agent.AgentExecution}. */
public record AgentExecutionId(UUID value) {

    public AgentExecutionId {
        Objects.requireNonNull(value, "user id must not be null");
    }

    public static AgentExecutionId of(UUID value) {
        return new AgentExecutionId(value);
    }

    public static AgentExecutionId of(String value) {
        return new AgentExecutionId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
