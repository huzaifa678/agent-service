package com.project.agent.application.conversation.service;

import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.ConversationNotFoundException;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConversationQueryServiceTest {

    @Mock
    private ConversationRepositoryPort conversationRepository;

    @InjectMocks
    private ConversationQueryService service;

    private static Conversation conversation(UUID id) {
        return new Conversation(
                ConversationId.of(id),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Test")
        );
    }

    @Test
    void getById_returnsConversation() {
        UUID id = UUID.randomUUID();
        Conversation conversation = conversation(id);

        when(conversationRepository.findById(ConversationId.of(id)))
                .thenReturn(Optional.of(conversation));

        Conversation result = service.getById(id);

        assertSame(conversation, result);
        verify(conversationRepository).findById(ConversationId.of(id));
    }

    @Test
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();

        when(conversationRepository.findById(ConversationId.of(id)))
                .thenReturn(Optional.empty());

        assertThrows(
                ConversationNotFoundException.class,
                () -> service.getById(id)
        );
    }

    @Test
    void byUser_returnsConversations() {
        UUID userId = UUID.randomUUID();

        List<Conversation> conversations = List.of(
                conversation(UUID.randomUUID()),
                conversation(UUID.randomUUID())
        );

        when(conversationRepository.findByUserId(UserId.of(userId)))
                .thenReturn(conversations);

        List<Conversation> result = service.byUser(userId);

        assertEquals(2, result.size());
        assertSame(conversations, result);
        verify(conversationRepository).findByUserId(UserId.of(userId));
    }

    @Test
    void messages_returnsConversationMessages() {
        UUID id = UUID.randomUUID();
        Conversation conversation = conversation(id);

        conversation.addMessage(new Message(
                MessageId.of(id),
                MessageContent.of("Hello"),
                MessageRole.USER,
                TokenUsage.of(1, 0)
        ));

        conversation.addMessage(new Message(
                MessageId.of(id),
                MessageContent.of("Hi!"),
                MessageRole.ASSISTANT,
                TokenUsage.of(2, 3)
        ));

        when(conversationRepository.findById(ConversationId.of(id)))
                .thenReturn(Optional.of(conversation));

        List<Message> messages = service.messages(id);

        assertEquals(2, messages.size());
        assertEquals("Hello", messages.get(0).getContent().value());
        assertEquals("Hi!", messages.get(1).getContent().value());

        verify(conversationRepository).findById(ConversationId.of(id));
    }

    @Test
    void messages_conversationNotFound_throws() {
        UUID id = UUID.randomUUID();

        when(conversationRepository.findById(ConversationId.of(id)))
                .thenReturn(Optional.empty());

        assertThrows(
                ConversationNotFoundException.class,
                () -> service.messages(id)
        );
    }
}
