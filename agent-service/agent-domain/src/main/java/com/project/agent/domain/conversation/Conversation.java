package com.project.agent.domain.conversation;

import com.project.agent.domain.message.Message;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import com.project.agent.domain.vo.shared.AbstractAggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Conversation aggregate root. Owns an ordered list of {@link Message} entities
 * and enforces the invariant that only active conversations may receive new messages.
 * Status transitions (active → archived, active → deleted) are the only mutations
 * allowed on a non-active conversation; all message-mutating operations call
 * {@link #ensureActive()} first.
 */
public class Conversation extends AbstractAggregateRoot {

    private final ConversationId id;

    private final TenantId tenantId;

    private final UserId userId;

    private ConversationTitle title;

    private ConversationStatus status;

    private final List<Message> messages;

    private final Instant createdAt;

    private Instant updatedAt;


    public Conversation(
            ConversationId id,
            TenantId tenantId,
            UserId userId,
            ConversationTitle title
    ) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.userId = Objects.requireNonNull(userId);
        this.title = title;
        this.status = ConversationStatus.ACTIVE;
        this.messages = new ArrayList<Message>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private Conversation(
            ConversationId id,
            TenantId tenantId,
            UserId userId,
            ConversationTitle title,
            ConversationStatus status,
            List<Message> messages,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = Objects.requireNonNull(tenantId);
        this.userId = Objects.requireNonNull(userId);
        this.title = title;
        this.status = Objects.requireNonNull(status);
        this.messages = new ArrayList<>(messages);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    /**
     * Rehydrate a persisted conversation with its stored state and timestamps,
     * bypassing lifecycle side-effects. For use by the persistence adapter only.
     */
    public static Conversation reconstitute(
            ConversationId id,
            TenantId tenantId,
            UserId userId,
            ConversationTitle title,
            ConversationStatus status,
            List<Message> messages,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Conversation(id, tenantId, userId, title, status, messages, createdAt, updatedAt);
    }

    public ConversationId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public UserId getUserId() {
        return userId;
    }

    public ConversationTitle getTitle() {
        return title;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Returns an unmodifiable view of all messages in chronological order. */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /** Rename the conversation; conversation must be active. */
    public void rename(ConversationTitle newTitle) {
        this.title = newTitle;
        touch();
    }

    /** Transition to {@link ConversationStatus#ARCHIVED}; conversation must be active. */
    public void archive() {

        ensureActive();

        this.status = ConversationStatus.ARCHIVED;
        touch();
    }

    /** Soft-delete the conversation regardless of current status. */
    public void delete() {

        this.status = ConversationStatus.DELETED;
        touch();
    }

    /** Restore the conversation to {@link ConversationStatus#ACTIVE}. */
    public void activate() {

        this.status = ConversationStatus.ACTIVE;
        touch();
    }

    /** Append a message to the conversation; conversation must be active. */
    public void addMessage(Message message) {

        Objects.requireNonNull(message);

        ensureActive();

        messages.add(message);

        touch();
    }

    /** Remove a message from the conversation. */
    public void removeMessage(Message message) {

        Objects.requireNonNull(message);

        messages.remove(message);

        touch();
    }

    /** Returns the most recently added message, or {@code null} if there are none. */
    public Message latestMessage() {

        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    /** Returns the total number of messages in this conversation. */
    public int messageCount() {
        return messages.size();
    }

    public boolean isActive() {
        return status == ConversationStatus.ACTIVE;
    }

    /** Stamp {@code updatedAt} with the current wall-clock time. */
    private void touch() {
        this.updatedAt = Instant.now();
    }

    private void ensureActive() {

        if (status != ConversationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Conversation is not active."
            );
        }
    }
}
