package com.project.agent.application.shared.port.out;

import com.project.agent.domain.vo.shared.DomainEvent;

import java.util.Collection;

/**
 * Outbound port for re-publishing domain events collected by aggregates.
 * Implemented by the adapter layer (e.g. a Kafka/Avro publisher) — the application
 * services drain an aggregate's events after it is persisted and hand them here.
 */
public interface DomainEventPublisherPort {

    /** Publish every event in the given collection; a no-op for an empty collection. */
    void publishAll(Collection<? extends DomainEvent> events);
}
