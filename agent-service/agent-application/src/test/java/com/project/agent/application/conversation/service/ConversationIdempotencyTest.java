package com.project.agent.application.conversation.service;

import com.project.agent.application.conversation.port.in.AddMessageCommand;
import com.project.agent.application.conversation.port.in.StartConversationCommand;
import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.domain.conversation.Conversation;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A retried start / add-message carrying the same Idempotency-Key must not duplicate work. */
@ExtendWith(MockitoExtension.class)
class ConversationIdempotencyTest {

    @Mock
    private ConversationRepositoryPort conversationRepository;

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
    void addMessage_sameIdempotencyKey_appendsOnce() {
        UUID id = UUID.randomUUID();
        Conversation conv = conversation(id);
        when(conversationRepository.findById(ConversationId.of(id))).thenReturn(Optional.of(conv));
        when(conversationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AddMessageCommand retried = new AddMessageCommand(id, "USER", "hi", 1, 0, "key-123");
        service.addMessage(retried);
        Conversation result = service.addMessage(retried); // retry with the same key

        assertEquals(1, result.messageCount());
        verify(conversationRepository, times(1)).save(any());
    }

    @Test
    void start_sameIdempotencyKey_returnsExistingOnReplay() {
        StartConversationCommand cmd =
                new StartConversationCommand(UUID.randomUUID(), UUID.randomUUID(), "Hello", "key-abc");

        // First call: the deterministic id isn't stored yet, so it creates and saves.
        when(conversationRepository.findById(any())).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Conversation first = service.start(cmd);

        // Replay: the same key resolves to the now-existing conversation; no second save.
        when(conversationRepository.findById(any())).thenReturn(Optional.of(first));
        Conversation replay = service.start(cmd);

        assertEquals(first.getId(), replay.getId());
        verify(conversationRepository, times(1)).save(any());
    }
}
