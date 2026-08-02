package com.project.agent.application.conversation.service;

import com.project.agent.application.conversation.port.in.AddMessageCommand;
import com.project.agent.application.conversation.port.in.StartConversationCommand;
import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.application.shared.port.out.DomainEventPublisherPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.ConversationAlreadyArchivedException;
import com.project.agent.domain.conversation.exception.ConversationDeletedException;
import com.project.agent.domain.conversation.exception.ConversationNotFoundException;
import com.project.agent.domain.conversation.exception.InvalidConversationStateException;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationCommandServiceTest {

    @Mock
    private ConversationRepositoryPort conversationRepository;
    @Mock
    private DomainEventPublisherPort eventPublisher;

    @InjectMocks
    private ConversationCommandService service;

    private static Conversation conversation(UUID id) {
        return new Conversation(
                ConversationId.of(id),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Test")
        );
    }

    @Test
    void start_persistsAndPublishesEvents() {
        UUID tenantId = UUID.randomUUID();
        when(conversationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Conversation result = service.start(new StartConversationCommand(tenantId, UUID.randomUUID(), "Hello"));

        assertNotNull(result);
        assertEquals(tenantId, result.getTenantId().value());
        verify(conversationRepository).save(any());
        verify(eventPublisher).publishAll(any());
    }

    @Test
    void addMessage_toActiveConversation_saves() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findById(ConversationId.of(id))).thenReturn(Optional.of(conversation(id)));
        when(conversationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Conversation result = service.addMessage(new AddMessageCommand(id, "USER", "hi", 1, 0));

        assertEquals(1, result.messageCount());
        verify(conversationRepository).save(any());
    }

    @Test
    void addMessage_toDeletedConversation_throws() {
        UUID id = UUID.randomUUID();
        Conversation deleted = conversation(id);
        deleted.delete();
        when(conversationRepository.findById(ConversationId.of(id))).thenReturn(Optional.of(deleted));

        assertThrows(ConversationDeletedException.class,
                () -> service.addMessage(new AddMessageCommand(id, "USER", "hi", 0, 0)));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void addMessage_toArchivedConversation_throwsInvalidState() {
        UUID id = UUID.randomUUID();
        Conversation archived = conversation(id);
        archived.archive();
        when(conversationRepository.findById(ConversationId.of(id))).thenReturn(Optional.of(archived));

        assertThrows(InvalidConversationStateException.class,
                () -> service.addMessage(new AddMessageCommand(id, "USER", "hi", 0, 0)));
    }

    @Test
    void archive_alreadyArchived_throws() {
        UUID id = UUID.randomUUID();
        Conversation archived = conversation(id);
        archived.archive();
        when(conversationRepository.findById(ConversationId.of(id))).thenReturn(Optional.of(archived));

        assertThrows(ConversationAlreadyArchivedException.class, () -> service.archive(id));
    }

    @Test
    void addMessage_conversationNotFound_throws() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.findById(ConversationId.of(id))).thenReturn(Optional.empty());

        assertThrows(ConversationNotFoundException.class,
                () -> service.addMessage(new AddMessageCommand(id, "USER", "hi", 0, 0)));
    }
}
