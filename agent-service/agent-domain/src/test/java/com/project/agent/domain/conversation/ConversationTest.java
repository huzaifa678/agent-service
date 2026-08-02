package com.project.agent.domain.conversation;

import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {

    private static Conversation newConversation() {
        return new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Test")
        );
    }

    private static Message message() {
        return new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of("hi"),
                MessageRole.USER,
                TokenUsage.empty()
        );
    }

    @Test
    void newConversationIsActiveAndEmpty() {
        Conversation conversation = newConversation();
        assertTrue(conversation.isActive());
        assertEquals(ConversationStatus.ACTIVE, conversation.getStatus());
        assertEquals(0, conversation.messageCount());
    }

    @Test
    void addMessageAppendsAndBumpsCount() {
        Conversation conversation = newConversation();
        Message message = message();
        conversation.addMessage(message);
        assertEquals(1, conversation.messageCount());
        assertEquals(message, conversation.latestMessage());
    }

    @Test
    void addMessageOnArchivedIsRejected() {
        Conversation conversation = newConversation();
        conversation.archive();
        assertThrows(IllegalStateException.class, () -> conversation.addMessage(message()));
    }

    @Test
    void archiveTwiceIsRejectedByAggregateGuard() {
        Conversation conversation = newConversation();
        conversation.archive();
        assertEquals(ConversationStatus.ARCHIVED, conversation.getStatus());
        assertThrows(IllegalStateException.class, conversation::archive);
    }

    @Test
    void deleteIsAllowedFromAnyState() {
        Conversation conversation = newConversation();
        conversation.archive();
        conversation.delete();
        assertEquals(ConversationStatus.DELETED, conversation.getStatus());
        assertFalse(conversation.isActive());
    }

    @Test
    void reconstitutePreservesStateAndMessages() {
        ConversationId id = ConversationId.of(UUID.randomUUID());
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        Instant updated = Instant.parse("2026-01-02T00:00:00Z");

        Conversation conversation = Conversation.reconstitute(
                id,
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Restored"),
                ConversationStatus.ARCHIVED,
                List.of(message()),
                created,
                updated
        );

        assertEquals(id, conversation.getId());
        assertEquals(ConversationStatus.ARCHIVED, conversation.getStatus());
        assertEquals(1, conversation.messageCount());
        assertEquals(created, conversation.getCreatedAt());
        assertEquals(updated, conversation.getUpdatedAt());
    }
}
