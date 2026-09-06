package com.project.agent.application.execution.service.harness;

import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryRateLimiterTest {

    @Test
    void allowsUpToCapacityThenRejects() {

        // capacity 3, refilling slowly enough that it won't top up during the test.
        MemoryRateLimiter limiter = new MemoryRateLimiter(true, 3, 3, 3600);
        ConversationId conversation = ConversationId.of(UUID.randomUUID());

        assertThat(limiter.tryAcquire(conversation)).isTrue();
        assertThat(limiter.tryAcquire(conversation)).isTrue();
        assertThat(limiter.tryAcquire(conversation)).isTrue();
        assertThat(limiter.tryAcquire(conversation)).isFalse();
    }

    @Test
    void budgetsArePerConversation() {

        MemoryRateLimiter limiter = new MemoryRateLimiter(true, 1, 1, 3600);
        ConversationId a = ConversationId.of(UUID.randomUUID());
        ConversationId b = ConversationId.of(UUID.randomUUID());

        assertThat(limiter.tryAcquire(a)).isTrue();
        assertThat(limiter.tryAcquire(a)).isFalse();
        // b has its own untouched bucket.
        assertThat(limiter.tryAcquire(b)).isTrue();
    }

    @Test
    void disabledLimiterAlwaysAllows() {

        MemoryRateLimiter limiter = new MemoryRateLimiter(false, 1, 1, 3600);
        ConversationId conversation = ConversationId.of(UUID.randomUUID());

        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(conversation)).isTrue();
        }
    }
}
