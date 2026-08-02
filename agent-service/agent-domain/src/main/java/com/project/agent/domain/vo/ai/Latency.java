package com.project.agent.domain.vo.ai;

import java.time.Duration;
import java.util.Objects;

/**
 * Value object representing the wall-clock duration of an LLM or tool call.
 * Wraps {@link java.time.Duration}; negative values are rejected. Computed
 * automatically by {@link com.project.agent.domain.execution.agent.AgentExecution}
 * and {@link com.project.agent.domain.execution.tool.ToolExecution} at completion time.
 */
public record Latency(Duration value) {

    public Latency {

        Objects.requireNonNull(value);

        if (value.isNegative()) {
            throw new IllegalArgumentException(
                    "Latency cannot be negative."
            );
        }
    }

    /** Returns the latency expressed in milliseconds. */
    public long toMillis() {
        return value.toMillis();
    }

    public static Latency of(Duration value){
        return new Latency(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
