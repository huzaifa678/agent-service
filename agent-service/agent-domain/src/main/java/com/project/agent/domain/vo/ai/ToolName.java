package com.project.agent.domain.vo.ai;

import java.util.Objects;

/** Value object for the registered name of a tool callable by the agent (e.g. {@code web_search}). */
public record ToolName(String value) {

    public ToolName {

        Objects.requireNonNull(value);

        if (value.isBlank()) {
            throw new IllegalArgumentException("Tool name cannot be blank.");
        }
    }

    public static ToolName of(String value) {
        return new ToolName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
