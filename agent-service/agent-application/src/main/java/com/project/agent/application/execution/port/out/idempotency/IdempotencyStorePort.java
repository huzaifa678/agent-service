package com.project.agent.application.execution.port.out.idempotency;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for request idempotency on unsafe, side-effecting operations — notably an agent
 * run, which spends on the LLM. The key (the forwarded {@code Idempotency-Key}) is reserved before
 * the work starts and completed after it succeeds, so a retry replays the recorded result rather
 * than re-running.
 *
 * <p>{@link #reserve} and {@link #release} must commit independently of the caller's transaction
 * (so a concurrent retry observes the reservation, and a failed run's reservation is dropped even
 * though the caller's transaction rolls back). {@link #complete} joins the caller's transaction, so
 * the completion and the work it records commit atomically.
 */
public interface IdempotencyStorePort {

    enum Status { IN_PROGRESS, COMPLETED }

    /** A stored idempotency entry. {@code resultId} is set only once {@link Status#COMPLETED}. */
    record Entry(String key, String fingerprint, Status status, UUID resultId) {}

    /** The stored entry for {@code key}, or empty if none exists or an IN_PROGRESS one has expired. */
    Optional<Entry> find(String key);

    /**
     * Insert an IN_PROGRESS reservation for {@code key} iff none exists (or the existing one has
     * expired and can be reclaimed). Returns true if this caller won the reservation.
     */
    boolean reserve(String key, String fingerprint, Duration ttl);

    /** Mark the reservation COMPLETED and point it at the result that a retry should replay. */
    void complete(String key, UUID resultId);

    /** Drop the reservation (on failure) so the operation can be retried. */
    void release(String key);
}
