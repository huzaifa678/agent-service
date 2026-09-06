package com.project.agent.application.execution.service.harness;

import com.project.agent.domain.vo.identity.ConversationId;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-conversation rate limiting for long-term memory operations, backed by Bucket4j token
 * buckets held in process.
 *
 * <p>Long-term recall and persist are the expensive memory operations — each one embeds text
 * and hits pgvector (and, with governance on, OPA). A runaway agent loop or an abusive client
 * could hammer them; the bucket puts a per-conversation ceiling on how often they run. Each
 * operation costs one token, a conversation gets {@code capacity} tokens, and they refill at
 * {@code refill-tokens} per {@code refill-period}.
 *
 * <p>This is a protective ceiling, not an authorization check: when a conversation is out of
 * tokens the harness quietly skips that turn's long-term work (recall degrades to the
 * short-term window; a persist is dropped) rather than failing the turn. The circuit breakers
 * handle dependency outages; this handles volume.
 */
@Component
public class MemoryRateLimiter {

    private final boolean enabled;
    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public MemoryRateLimiter(
            @Value("${agent.memory.rate-limit.enabled:true}") boolean enabled,
            @Value("${agent.memory.rate-limit.capacity:30}") long capacity,
            @Value("${agent.memory.rate-limit.refill-tokens:30}") long refillTokens,
            @Value("${agent.memory.rate-limit.refill-period-seconds:60}") long refillPeriodSeconds
    ) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = Duration.ofSeconds(refillPeriodSeconds);
    }

    /**
     * Try to spend one token from a conversation's memory budget.
     *
     * @return {@code true} if the operation may proceed, {@code false} if the conversation is
     *         currently rate-limited (or always {@code true} when disabled)
     */
    public boolean tryAcquire(ConversationId conversationId) {
        if (!enabled) {
            return true;
        }
        Bucket bucket =
                buckets.computeIfAbsent(conversationId.toString(), key -> newBucket());
        return bucket.tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, refillPeriod)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
