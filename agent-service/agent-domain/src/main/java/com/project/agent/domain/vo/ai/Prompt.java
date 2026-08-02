package com.project.agent.domain.vo.ai;

import java.util.Objects;

/** Value object for a system or user prompt sent to the LLM. Must be non-blank. */
public record Prompt(String value) {

    public Prompt {

        Objects.requireNonNull(value);

        if (value.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be blank");
        }
    }

    public static Prompt of(String value) {
        return new Prompt(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
