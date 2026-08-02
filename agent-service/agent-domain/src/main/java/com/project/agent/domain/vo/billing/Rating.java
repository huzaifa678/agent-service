package com.project.agent.domain.vo.billing;

/**
 * Value object representing a user-submitted quality rating for an agent response.
 * Valid range is 1–5 inclusive; {@link #positive()} returns {@code true} for ratings
 * of 4 or higher, which can be used to filter high-quality responses for fine-tuning.
 */
public record Rating(int value) {

    public Rating {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5."
            );
        }
    }

    public static Rating of(int value) {
        return new Rating(value);
    }

    /** Returns {@code true} if the rating is 4 or 5 (considered positive feedback). */
    public boolean positive() {
        return value >= 4;
    }
}
