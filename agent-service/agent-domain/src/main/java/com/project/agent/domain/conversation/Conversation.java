package com.project.agent.domain.conversation;

import com.project.agent.domain.conversation.event.ConversationEvent;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import com.project.agent.domain.vo.shared.AbstractAggregateRoot;
import com.project.agent.domain.vo.shared.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Conversation aggregate root, modelled as an event-sourced aggregate. Behaviour methods
 * never mutate fields directly: they validate invariants, then raise a {@link ConversationEvent}
 * that is both applied to in-memory state and recorded for persistence. Current state is
 * therefore always the fold of the aggregate's event stream, and can be rebuilt by
 * {@link #replay(List)} (optionally onto a snapshot via {@link #fromSnapshot} + {@link #replayAll}).
 *
 * <p>Only active conversations may receive new messages; status transitions
 * (active → archived, any → deleted, → active) are the exceptions allowed on a non-active
 * conversation. {@link #version} is the aggregate-local version and the basis for optimistic
 * concurrency when the repository appends new events.
 */
public class Conversation extends AbstractAggregateRoot {

    private final ConversationId id;

    private TenantId tenantId;

    private UserId userId;

    private ConversationTitle title;

    private ConversationStatus status;

    private final List<Message> messages = new ArrayList<>();

    private Instant createdAt;

    private Instant updatedAt;

    /** Aggregate-local version: the sequence of the last applied event (0 for a new instance). */
    private long version;

    /** Start a brand-new conversation, raising {@link ConversationEvent.Started}. */
    public Conversation(
            ConversationId id,
            TenantId tenantId,
            UserId userId,
            ConversationTitle title
    ) {
        this.id = Objects.requireNonNull(id);
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(userId);
        applyChange(ConversationEvent.Started.of(
                id.value(),
                nextSequence(),
                tenantId.value(),
                userId.value(),
                title == null ? null : title.value()
        ));
    }

    /** Private constructor used only when rebuilding from events or a snapshot. */
    private Conversation(ConversationId id) {
        this.id = Objects.requireNonNull(id);
    }

    /**
     * Rehydrate a conversation from an already-materialised state (the read-model
     * projection), bypassing lifecycle side-effects and raising no events. Persistence
     * read-model use only — the returned aggregate is not intended to be mutated and saved.
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
        return fromSnapshot(id, tenantId, userId, title, status, messages, createdAt, updatedAt, 0L);
    }

    /**
     * Rehydrate a conversation from a stored snapshot at a known {@code version}. The
     * repository then applies any events after that version with {@link #replayAll(List)}.
     */
    public static Conversation fromSnapshot(
            ConversationId id,
            TenantId tenantId,
            UserId userId,
            ConversationTitle title,
            ConversationStatus status,
            List<Message> messages,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        Conversation conversation = new Conversation(id);
        conversation.tenantId = Objects.requireNonNull(tenantId);
        conversation.userId = Objects.requireNonNull(userId);
        conversation.title = title;
        conversation.status = Objects.requireNonNull(status);
        conversation.messages.addAll(messages);
        conversation.createdAt = Objects.requireNonNull(createdAt);
        conversation.updatedAt = Objects.requireNonNull(updatedAt);
        conversation.version = version;
        return conversation;
    }

    /**
     * Rebuild a conversation from its full event stream. The first event must be a
     * {@link ConversationEvent.Started}. Raises no new events — replay is not a mutation.
     */
    public static Conversation replay(List<ConversationEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Cannot replay a conversation from an empty event stream.");
        }
        ConversationEvent first = events.get(0);
        Conversation conversation = new Conversation(ConversationId.of(first.conversationId()));
        conversation.replayAll(events);
        return conversation;
    }

    /** Apply a tail of events (e.g. those after a snapshot) without recording them. */
    public void replayAll(List<ConversationEvent> events) {
        for (ConversationEvent event : events) {
            apply(event);
        }
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

    /** Aggregate-local version (sequence of the last applied event). */
    public long getVersion() {
        return version;
    }

    /** Returns an unmodifiable view of all messages in chronological order. */
    public List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /** Rename the conversation. */
    public void rename(ConversationTitle newTitle) {
        applyChange(ConversationEvent.Renamed.of(
                id.value(),
                nextSequence(),
                newTitle == null ? null : newTitle.value()
        ));
    }

    /** Transition to {@link ConversationStatus#ARCHIVED}; conversation must be active. */
    public void archive() {
        ensureActive();
        applyChange(ConversationEvent.Archived.of(id.value(), nextSequence()));
    }

    /** Soft-delete the conversation regardless of current status. */
    public void delete() {
        applyChange(ConversationEvent.Deleted.of(id.value(), nextSequence()));
    }

    /** Restore the conversation to {@link ConversationStatus#ACTIVE}. */
    public void activate() {
        applyChange(ConversationEvent.Activated.of(id.value(), nextSequence()));
    }

    /** Append a message to the conversation; conversation must be active. */
    public void addMessage(Message message) {
        Objects.requireNonNull(message);
        ensureActive();
        applyChange(ConversationEvent.MessageAdded.of(
                id.value(),
                nextSequence(),
                message.getId().value(),
                message.getContent().value(),
                message.getRole().name(),
                message.getTokenUsage().promptTokens(),
                message.getTokenUsage().completionTokens(),
                message.getCreatedAt()
        ));
    }

    /** Remove a message from the conversation. */
    public void removeMessage(Message message) {
        Objects.requireNonNull(message);
        applyChange(ConversationEvent.MessageRemoved.of(id.value(), nextSequence(), message.getId().value()));
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

    /**
     * Drain the events raised since this aggregate was loaded, typed to
     * {@link ConversationEvent}. The event-sourced repository calls this exactly once
     * per {@code save} to append them to the store.
     */
    public List<ConversationEvent> pullPendingEvents() {
        List<DomainEvent> drained = drainDomainEvents();
        List<ConversationEvent> events = new ArrayList<>(drained.size());
        for (DomainEvent event : drained) {
            events.add((ConversationEvent) event);
        }
        return events;
    }

    /** Validate, then apply-and-record a new event. */
    private void applyChange(ConversationEvent event) {
        apply(event);
        registerEvent(event);
    }

    /**
     * Fold one event into in-memory state, then advance the version. The switch is only a
     * type-safe dispatch table — each event's evolution logic lives in its own focused method.
     * A pattern switch over the sealed event type (rather than a State-pattern hierarchy) is the
     * idiomatic fold for an event-sourced aggregate: the compiler forces every event to be
     * handled, and adding an event is a compile error until it is. Never raises further events.
     */
    private void apply(ConversationEvent event) {
        switch (event) {
            case ConversationEvent.Started e -> applyStarted(e);
            case ConversationEvent.Renamed e -> applyRenamed(e);
            case ConversationEvent.MessageAdded e -> applyMessageAdded(e);
            case ConversationEvent.MessageRemoved e -> applyMessageRemoved(e);
            case ConversationEvent.Archived e -> applyArchived(e);
            case ConversationEvent.Deleted e -> applyDeleted(e);
            case ConversationEvent.Activated e -> applyActivated(e);
        }
        this.version = event.sequence();
    }

    private void applyStarted(ConversationEvent.Started e) {
        this.tenantId = TenantId.of(e.tenantId());
        this.userId = UserId.of(e.userId());
        this.title = e.title() == null ? null : ConversationTitle.of(e.title());
        this.status = ConversationStatus.ACTIVE;
        this.createdAt = e.occurredAt();
        this.updatedAt = e.occurredAt();
    }

    private void applyRenamed(ConversationEvent.Renamed e) {
        this.title = e.title() == null ? null : ConversationTitle.of(e.title());
        this.updatedAt = e.occurredAt();
    }

    private void applyMessageAdded(ConversationEvent.MessageAdded e) {
        this.messages.add(Message.reconstitute(
                MessageId.of(e.messageId()),
                MessageContent.of(e.content()),
                MessageRole.valueOf(e.role()),
                TokenUsage.of(e.promptTokens(), e.completionTokens()),
                e.messageCreatedAt()
        ));
        this.updatedAt = e.occurredAt();
    }

    private void applyMessageRemoved(ConversationEvent.MessageRemoved e) {
        this.messages.removeIf(m -> m.getId().value().equals(e.messageId()));
        this.updatedAt = e.occurredAt();
    }

    private void applyArchived(ConversationEvent.Archived e) {
        this.status = ConversationStatus.ARCHIVED;
        this.updatedAt = e.occurredAt();
    }

    private void applyDeleted(ConversationEvent.Deleted e) {
        this.status = ConversationStatus.DELETED;
        this.updatedAt = e.occurredAt();
    }

    private void applyActivated(ConversationEvent.Activated e) {
        this.status = ConversationStatus.ACTIVE;
        this.updatedAt = e.occurredAt();
    }

    private long nextSequence() {
        return version + 1;
    }

    private void ensureActive() {
        if (status != ConversationStatus.ACTIVE) {
            throw new IllegalStateException("Conversation is not active.");
        }
    }
}
