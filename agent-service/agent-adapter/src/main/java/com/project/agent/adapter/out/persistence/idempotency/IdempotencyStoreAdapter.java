package com.project.agent.adapter.out.persistence.idempotency;

import com.project.agent.application.execution.port.out.idempotency.IdempotencyStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed {@link IdempotencyStorePort}.
 *
 * <p>{@link #reserve} and {@link #release} run in their own transaction (REQUIRES_NEW) so the
 * reservation is visible to a concurrent retry and a failed run's reservation is dropped even when
 * the caller's transaction rolls back. {@link #complete} joins the caller's transaction, so it
 * commits atomically with the execution it records.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyStoreAdapter implements IdempotencyStorePort {

    private final IdempotencyKeyJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Entry> find(String key) {
        return repository.findById(key)
                .filter(entity -> !isExpiredReservation(entity, Instant.now()))
                .map(entity -> new Entry(
                        entity.getIdempotencyKey(),
                        entity.getFingerprint(),
                        Status.valueOf(entity.getStatus()),
                        entity.getResultId()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reserve(String key, String fingerprint, Duration ttl) {
        Instant now = Instant.now();
        return repository.tryReserve(key, fingerprint, now.plus(ttl), now) > 0;
    }

    @Override
    public void complete(String key, UUID resultId) {
        repository.findById(key).ifPresent(entity -> {
            entity.setStatus(Status.COMPLETED.name());
            entity.setResultId(resultId);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key) {
        repository.deleteById(key);
    }

    private static boolean isExpiredReservation(IdempotencyKeyEntity entity, Instant now) {
        return Status.IN_PROGRESS.name().equals(entity.getStatus())
                && entity.getExpiresAt().isBefore(now);
    }
}
