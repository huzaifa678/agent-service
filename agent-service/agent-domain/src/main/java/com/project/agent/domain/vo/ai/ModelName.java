package com.project.agent.domain.vo.ai;

import java.util.Objects;

/** Value object for the name of an LLM model (e.g. {@code gpt-4o}, {@code claude-3-5-sonnet}). */
public record ModelName(String value) {

    public ModelName {
        Objects.requireNonNull(value, "model name cannot be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Model name cannot be blank");
        }
    }

    public static ModelName of(String value) {
        return new ModelName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
