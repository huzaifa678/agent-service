package com.project.agent.application.execution.service.common.builder;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;

import java.util.UUID;

/**
 * Test builder for {@link Conversation}.
 */
public class ConversationBuilder {

    private UUID conversationId = UUID.randomUUID();
    private UUID tenantId = UUID.randomUUID();
    private UUID userId = UUID.randomUUID();
    private String title = "Test Conversation";

    private boolean archived;
    private boolean deleted;

    public static ConversationBuilder aConversation() {
        return new ConversationBuilder();
    }

    public ConversationBuilder withConversationId(UUID id) {
        this.conversationId = id;
        return this;
    }

    public ConversationBuilder withTenantId(UUID id) {
        this.tenantId = id;
        return this;
    }

    public ConversationBuilder withUserId(UUID id) {
        this.userId = id;
        return this;
    }

    public ConversationBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public ConversationBuilder archived() {
        this.archived = true;
        return this;
    }

    public ConversationBuilder deleted() {
        this.deleted = true;
        return this;
    }

    public Conversation build() {

        Conversation conversation = new Conversation(
                ConversationId.of(conversationId),
                TenantId.of(tenantId),
                UserId.of(userId),
                ConversationTitle.of(title)
        );

        if (archived) {
            conversation.archive();
        }

        if (deleted) {
            conversation.delete();
        }

        return conversation;
    }
}
