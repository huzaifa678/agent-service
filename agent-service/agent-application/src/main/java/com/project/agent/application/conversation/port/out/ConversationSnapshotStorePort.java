package com.project.agent.application.conversation.port.out;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.vo.identity.ConversationId;

import java.util.Optional;

/**
 * Outbound port for conversation snapshots. A snapshot is a materialised aggregate state at a
 * known version; loading a snapshot and replaying only the events after it bounds replay cost
 * for long-lived streams. Snapshots are a pure optimisation — they can be dropped and
 * rebuilt from the event store at any time.
 */
public interface ConversationSnapshotStorePort {

    /** Load the latest snapshot for an aggregate, if one exists. */
    Optional<Conversation> load(ConversationId aggregateId);

    /** Persist (upsert) a snapshot capturing the aggregate's current state and version. */
    void save(Conversation conversation);
}
