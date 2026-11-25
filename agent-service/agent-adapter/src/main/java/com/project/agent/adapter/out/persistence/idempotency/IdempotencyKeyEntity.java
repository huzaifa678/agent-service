package com.project.agent.adapter.out.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Request-idempotency record for a side-effecting operation. One row per {@code Idempotency-Key}:
 * reserved IN_PROGRESS before the work runs, then COMPLETED with the {@code result_id} it produced.
 * {@code expiresAt} bounds a stuck reservation so a crashed run does not block retries forever.
 * Matches the {@code idempotency_keys} table (Liquibase changeset 011).
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
