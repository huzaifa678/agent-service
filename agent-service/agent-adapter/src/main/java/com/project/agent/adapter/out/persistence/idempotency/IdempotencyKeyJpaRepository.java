package com.project.agent.adapter.out.persistence.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/** Spring Data repository over the {@code idempotency_keys} table. */
public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, String> {

    /**
     * Atomically reserve {@code key} as IN_PROGRESS. Inserts a fresh row, or reclaims an existing
     * one only if it is an expired reservation; a live reservation or a completed result leaves the
     * row untouched. Postgres evaluates this as one statement, so concurrent callers can't both win,
     * and a conflict is resolved without raising (no transaction is poisoned). Returns the number of
     * rows written: 1 if this caller reserved the key, 0 otherwise.
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys
                (idempotency_key, fingerprint, status, result_id, expires_at, created_at, updated_at)
            VALUES (:key, :fingerprint, 'IN_PROGRESS', NULL, :expiresAt, :now, :now)
            ON CONFLICT (idempotency_key) DO UPDATE
               SET fingerprint = EXCLUDED.fingerprint,
                   status      = 'IN_PROGRESS',
                   result_id   = NULL,
                   expires_at  = EXCLUDED.expires_at,
                   updated_at  = EXCLUDED.updated_at
             WHERE idempotency_keys.status = 'IN_PROGRESS'
               AND idempotency_keys.expires_at < :now
            """, nativeQuery = true)
    int tryReserve(@Param("key") String key,
                   @Param("fingerprint") String fingerprint,
                   @Param("expiresAt") Instant expiresAt,
                   @Param("now") Instant now);
}
