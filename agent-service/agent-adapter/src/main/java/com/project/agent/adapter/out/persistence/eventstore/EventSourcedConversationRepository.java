package com.project.agent.adapter.out.persistence.eventstore;

import com.project.agent.application.conversation.port.out.ConversationEventStorePort;
import com.project.agent.application.conversation.port.out.ConversationProjectionPort;
import com.project.agent.application.conversation.port.out.ConversationReadModelPort;
import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.application.conversation.port.out.ConversationSnapshotStorePort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.event.ConversationEvent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.UserId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Event-sourced {@link ConversationRepositoryPort} — the command-side write repository and the
 * {@code @Primary} bean the application services inject.
 *
 * <p>{@code save} drains the aggregate's pending events, appends them to the event store at the
 * expected version (optimistic concurrency), updates the read-model projection, and periodically
 * writes a snapshot. All of this runs inside the calling command's transaction, so the event
 * store and the projection commit atomically. {@code findById} rebuilds the aggregate from its
 * latest snapshot plus the events after it (or the full stream when there is no snapshot).
 *
 * <p>Publishing to Kafka is deliberately <b>not</b> done here: {@link ConversationEventRelay}
 * relays the durably-stored events out of band, which removes the persist-then-publish dual write.
 */
@Component
@Primary
public class EventSourcedConversationRepository implements ConversationRepositoryPort {

    private final ConversationEventStorePort eventStore;
    private final ConversationSnapshotStorePort snapshotStore;
    private final ConversationProjectionPort projection;
    private final ConversationReadModelPort readModel;
    private final int snapshotInterval;

    public EventSourcedConversationRepository(
            ConversationEventStorePort eventStore,
            ConversationSnapshotStorePort snapshotStore,
            ConversationProjectionPort projection,
            ConversationReadModelPort readModel,
            @Value("${agent.conversation.snapshot-interval:50}") int snapshotInterval
    ) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.projection = projection;
        this.readModel = readModel;
        this.snapshotInterval = snapshotInterval;
    }

    @Override
    public Conversation save(Conversation conversation) {
        List<ConversationEvent> pending = conversation.pullPendingEvents();
        if (pending.isEmpty()) {
            return conversation;
        }

        long newVersion = conversation.getVersion();
        long expectedVersion = newVersion - pending.size();

        eventStore.append(conversation.getId(), expectedVersion, pending);
        projection.project(conversation);

        if (crossedSnapshotBoundary(expectedVersion, newVersion)) {
            snapshotStore.save(conversation);
        }
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        Optional<Conversation> snapshot = snapshotStore.load(id);
        if (snapshot.isPresent()) {
            Conversation conversation = snapshot.get();
            List<ConversationEvent> tail = eventStore.loadAfter(id, conversation.getVersion());
            conversation.replayAll(tail);
            return Optional.of(conversation);
        }

        List<ConversationEvent> events = eventStore.loadAfter(id, 0L);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Conversation.replay(events));
    }

    @Override
    public List<Conversation> findByUserId(UserId userId) {
        // Fan-out listing is served from the read model; it is a read, never a load-to-mutate.
        return readModel.findByUserId(userId);
    }

    /** True when appending up to {@code newVersion} passes a multiple of the snapshot interval. */
    private boolean crossedSnapshotBoundary(long expectedVersion, long newVersion) {
        if (snapshotInterval <= 0) {
            return false;
        }
        return Math.floorDiv(newVersion, snapshotInterval) > Math.floorDiv(expectedVersion, snapshotInterval);
    }
}
