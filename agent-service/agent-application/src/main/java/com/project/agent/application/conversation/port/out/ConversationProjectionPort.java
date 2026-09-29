package com.project.agent.application.conversation.port.out;

import com.project.agent.domain.conversation.Conversation;

/**
 * Outbound port for the conversation read-model projection. The event-sourced repository
 * calls this in the same transaction as the event append, so the {@code conversations}/
 * {@code messages} tables the query side reads stay consistent with the event store.
 */
public interface ConversationProjectionPort {

    /** Upsert the read-model rows to reflect the aggregate's current state. */
    void project(Conversation conversation);
}
