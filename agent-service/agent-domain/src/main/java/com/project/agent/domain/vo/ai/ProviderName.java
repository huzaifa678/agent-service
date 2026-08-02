package com.project.agent.domain.vo.ai;

import java.util.Objects;

/** Value object for the name of an AI provider (e.g. {@code openai}, {@code anthropic}). */
public record ProviderName(String value) {

    public ProviderName {

        Objects.requireNonNull(value);

        if (value.isBlank()) {
            throw new IllegalArgumentException("Provider name cannot be blank.");
        }
    }

    public static ProviderName of(String value) {
        return new ProviderName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
