package com.project.agent.domain.vo.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for aggregate roots. Collects domain events raised while handling
 * behaviour; the application layer drains them via {@link #clearDomainEvents()}
 * after the aggregate is persisted and publishes them through an outbound port.
 */
public abstract class AbstractAggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** Append a domain event to the aggregate's internal event list. */
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /** Returns an unmodifiable view of all pending domain events. */
    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /** Clear all pending domain events (call after publishing). */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * Atomically returns a snapshot of the pending events and clears the internal
     * list. Event-sourced repositories drain events this way so they are appended
     * to the event store exactly once per {@code save}.
     */
    protected List<DomainEvent> drainDomainEvents() {
        List<DomainEvent> drained = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return drained;
    }
}