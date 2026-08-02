package com.project.agent.domain.message;

import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.MessageId;
import com.project.agent.domain.vo.ai.TokenUsage;

import java.time.Instant;
import java.util.Objects;

/**
 * A single turn within a {@link com.project.agent.domain.conversation.Conversation}.
 * Immutable identity; content and token usage may be updated after creation to
 * reflect streaming completions or cost corrections. Equality is by {@link MessageId}.
 */
public class Message {

    private final MessageId id;

    private MessageContent content;

    private final MessageRole role;

    private TokenUsage tokenUsage;

    private final Instant createdAt;

    public Message(
            MessageId id,
            MessageContent content,
            MessageRole role,
            TokenUsage tokenUsage
    ) {
        this.id = Objects.requireNonNull(id, "Message id cannot be null.");
        this.content = Objects.requireNonNull(content, "Message content cannot be null");
        this.role = Objects.requireNonNull(role, "Message role cannot be null");
        this.tokenUsage = Objects.requireNonNull(tokenUsage, "Token usage cannot be null");
        this.createdAt = Instant.now();
    }

    private Message(
            MessageId id,
            MessageContent content,
            MessageRole role,
            TokenUsage tokenUsage,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.content = Objects.requireNonNull(content);
        this.role = Objects.requireNonNull(role);
        this.tokenUsage = Objects.requireNonNull(tokenUsage);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Rehydrate a persisted message with its original {@code createdAt}. Persistence-adapter use only. */
    public static Message reconstitute(
            MessageId id,
            MessageContent content,
            MessageRole role,
            TokenUsage tokenUsage,
            Instant createdAt
    ) {
        return new Message(id, content, role, tokenUsage, createdAt);
    }

    public MessageId getId() {
        return id;
    }

    public MessageContent getContent() {
        return content;
    }

    public MessageRole getRole() {
        return role;
    }

    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Replace the message body (e.g. after a streaming completion is finalised). */
    public void updateContent(MessageContent content) {
        this.content = Objects.requireNonNull(content);
    }

    /** Replace the token usage (e.g. after cost data is available from the provider). */
    public void updateTokenUsage(TokenUsage tokenUsage) {
        this.tokenUsage = Objects.requireNonNull(tokenUsage);
    }

    public boolean isUserMessage() {
        return role == MessageRole.USER;
    }

    public boolean isAssistantMessage() {
        return role == MessageRole.ASSISTANT;
    }

    public boolean isSystemMessage() {
        return role == MessageRole.SYSTEM;
    }

    public boolean isToolMessage() {
        return role == MessageRole.TOOL;
    }

    /** Returns the character length of the message content. */
    public int contentLength() {
        return content.length();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Message other)) {
            return false;
        }

        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
