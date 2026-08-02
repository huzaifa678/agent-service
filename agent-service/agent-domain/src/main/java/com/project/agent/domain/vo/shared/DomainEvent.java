package com.project.agent.domain.vo.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Marker interface for domain events raised by aggregates. Events are pure —
 * they carry value objects and primitives only, never framework or persistence
 * types — and are published by the application layer through an outbound port.
 */
public interface DomainEvent {

    /** Unique identifier for this event instance. */
    UUID eventId();

    /** Wall-clock time at which the event occurred. */
    Instant occurredAt();
}
