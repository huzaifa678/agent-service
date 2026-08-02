package com.project.agent.application.execution.service.common.builder;

import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.MessageId;

import java.util.UUID;

/**
 * Test builder for {@link Message}.
 */
public class MessageBuilder {

    private UUID id = UUID.randomUUID();
    private String content = "Hello";
    private MessageRole role = MessageRole.USER;
    private TokenUsage tokenUsage = TokenUsage.empty();

    public static MessageBuilder aMessage() {
        return new MessageBuilder();
    }

    public MessageBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public MessageBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public MessageBuilder withRole(MessageRole role) {
        this.role = role;
        return this;
    }

    public MessageBuilder withTokenUsage(TokenUsage tokenUsage) {
        this.tokenUsage = tokenUsage;
        return this;
    }

    public MessageBuilder user() {
        this.role = MessageRole.USER;
        return this;
    }

    public MessageBuilder assistant() {
        this.role = MessageRole.ASSISTANT;
        return this;
    }

    public MessageBuilder system() {
        this.role = MessageRole.SYSTEM;
        return this;
    }

    public Message build() {

        return new Message(
                MessageId.of(id),
                MessageContent.of(content),
                role,
                tokenUsage
        );
    }
}