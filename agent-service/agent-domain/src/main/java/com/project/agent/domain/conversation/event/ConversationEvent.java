package com.project.agent.domain.conversation.event;

import com.project.agent.domain.vo.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Sealed catalogue of the facts a {@link com.project.agent.domain.conversation.Conversation}
 * emits over its lifetime. These events are the aggregate's source of truth: current state is
 * rebuilt by replaying them in {@link #sequence()} order (optionally from a snapshot).
 *
 * <p>Every event is immutable and pure — it carries value-object primitives only, never
 * framework or persistence types — and is serialised verbatim into the event store. Because
 * the store is append-only, <b>existing records must never be reshaped</b>; evolve the model by
 * adding new event types and tolerating old ones on replay.
 *
 * <p>{@code sequence} is the aggregate-local version this event advances the conversation to
 * (1 for the first event). The pair {@code (conversationId, sequence)} is unique in the store,
 * which is what enforces optimistic concurrency on append.
 */
public sealed interface ConversationEvent extends DomainEvent {

    /** Identity of the conversation this event belongs to. */
    UUID conversationId();

    /** Aggregate-local version reached by applying this event (monotonic, gap-free, 1-based). */
    long sequence();

    /** The conversation was created. Always the first event in a stream. */
    record Started(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence,
            UUID tenantId,
            UUID userId,
            String title
    ) implements ConversationEvent {
        public static Started of(UUID conversationId, long sequence, UUID tenantId, UUID userId, String title) {
            return new Started(UUID.randomUUID(), Instant.now(), conversationId, sequence, tenantId, userId, title);
        }
    }

    /** The conversation title changed. */
    record Renamed(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence,
            String title
    ) implements ConversationEvent {
        public static Renamed of(UUID conversationId, long sequence, String title) {
            return new Renamed(UUID.randomUUID(), Instant.now(), conversationId, sequence, title);
        }
    }

    /** A message was appended to the conversation. */
    record MessageAdded(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence,
            UUID messageId,
            String content,
            String role,
            int promptTokens,
            int completionTokens,
            Instant messageCreatedAt
    ) implements ConversationEvent {
        public static MessageAdded of(
                UUID conversationId,
                long sequence,
                UUID messageId,
                String content,
                String role,
                int promptTokens,
                int completionTokens,
                Instant messageCreatedAt
        ) {
            return new MessageAdded(UUID.randomUUID(), Instant.now(), conversationId, sequence,
                    messageId, content, role, promptTokens, completionTokens, messageCreatedAt);
        }
    }

    /** A message was removed from the conversation. */
    record MessageRemoved(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence,
            UUID messageId
    ) implements ConversationEvent {
        public static MessageRemoved of(UUID conversationId, long sequence, UUID messageId) {
            return new MessageRemoved(UUID.randomUUID(), Instant.now(), conversationId, sequence, messageId);
        }
    }

    /** The conversation was archived (read-only). */
    record Archived(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence
    ) implements ConversationEvent {
        public static Archived of(UUID conversationId, long sequence) {
            return new Archived(UUID.randomUUID(), Instant.now(), conversationId, sequence);
        }
    }

    /** The conversation was soft-deleted. */
    record Deleted(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence
    ) implements ConversationEvent {
        public static Deleted of(UUID conversationId, long sequence) {
            return new Deleted(UUID.randomUUID(), Instant.now(), conversationId, sequence);
        }
    }

    /** The conversation was restored to active. */
    record Activated(
            UUID eventId,
            Instant occurredAt,
            UUID conversationId,
            long sequence
    ) implements ConversationEvent {
        public static Activated of(UUID conversationId, long sequence) {
            return new Activated(UUID.randomUUID(), Instant.now(), conversationId, sequence);
        }
    }
}
