package com.project.agent.domain.vo.ai;

/**
 * Value object capturing the token consumption of a single LLM call.
 * Both components are validated non-negative; {@link #totalTokens()} is derived.
 * Use {@link #empty()} for a zero-value placeholder before the provider responds.
 */
public record TokenUsage(
        int promptTokens,
        int completionTokens
) {

    public TokenUsage {

        if (promptTokens < 0) {
            throw new IllegalArgumentException("Prompt tokens cannot be negative");
        }

        if (completionTokens < 0) {
            throw new IllegalArgumentException("Completion tokens cannot be negative");
        }
    }

    /** Returns the sum of prompt and completion tokens. */
    public int totalTokens() {
        return promptTokens + completionTokens;
    }

    /** Zero-value instance used before actual usage data is available. */
    public static TokenUsage empty() {
        return new TokenUsage(0, 0);
    }

    public static TokenUsage of(int promptTokens, int completionTokens) {
        return new TokenUsage(promptTokens, completionTokens);
    }
}
