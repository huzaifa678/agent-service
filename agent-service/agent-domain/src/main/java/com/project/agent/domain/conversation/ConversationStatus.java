package com.project.agent.domain.conversation;

/** Lifecycle states for a {@link Conversation}. */
public enum ConversationStatus {
    /** Accepting new messages and agent executions. */
    ACTIVE,
    /** Read-only; no new messages permitted. */
    ARCHIVED,
    /** Soft-deleted; excluded from normal queries. */
    DELETED
}
