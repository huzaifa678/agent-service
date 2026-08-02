package com.project.agent.domain.vo.identity;

import java.util.Objects;
import java.util.UUID;

/** Value object identifying the end-user who owns a conversation. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "user id must not be null");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
