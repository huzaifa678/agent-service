package com.project.agent.application.conversation.service;

import com.project.agent.application.conversation.port.in.AddMessageCommand;
import com.project.agent.application.conversation.port.in.AddMessageUseCase;
import com.project.agent.application.conversation.port.in.ArchiveConversationUseCase;
import com.project.agent.application.conversation.port.in.DeleteConversationUseCase;
import com.project.agent.application.conversation.port.in.RenameConversationCommand;
import com.project.agent.application.conversation.port.in.RenameConversationUseCase;
import com.project.agent.application.conversation.port.in.StartConversationCommand;
import com.project.agent.application.conversation.port.in.StartConversationUseCase;
import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.ConversationAlreadyArchivedException;
import com.project.agent.domain.conversation.exception.ConversationDeletedException;
import com.project.agent.domain.conversation.exception.ConversationNotFoundException;
import com.project.agent.domain.conversation.exception.InvalidConversationStateException;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Write side of the conversation context. Each public method implements a single
 * inbound use-case; transaction boundaries live here. Guards translate the
 * conversation lifecycle into the rich domain exceptions before delegating to the
 * aggregate (whose own guards throw a generic {@link IllegalStateException}).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ConversationCommandService implements
        StartConversationUseCase,
        RenameConversationUseCase,
        AddMessageUseCase,
        ArchiveConversationUseCase,
        DeleteConversationUseCase {

    private final ConversationRepositoryPort conversationRepository;

    @Override
    public Conversation start(StartConversationCommand command) {
        ConversationId conversationId = idFor("conversation", command.idempotencyKey())
                .map(ConversationId::of)
                .orElseGet(() -> ConversationId.of(UUID.randomUUID()));

        // Replay: a retry carrying the same Idempotency-Key maps to the same id, so an
        // already-started conversation is returned instead of opening a second one.
        if (command.idempotencyKey() != null) {
            Optional<Conversation> existing = conversationRepository.findById(conversationId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Conversation conversation = new Conversation(
                conversationId,
                TenantId.of(command.tenantId()),
                UserId.of(command.userId()),
                ConversationTitle.of(command.title())
        );

        try {
            return conversationRepository.save(conversation);
        } catch (OptimisticLockingFailureException concurrentRetry) {
            // A concurrent retry won the race and created it first; return that one.
            return conversationRepository.findById(conversationId)
                    .orElseThrow(() -> concurrentRetry);
        }
    }

    @Override
    public void rename(RenameConversationCommand command) {
        Conversation conversation = load(ConversationId.of(command.conversationId()));
        if (conversation.getStatus() == com.project.agent.domain.conversation.ConversationStatus.DELETED) {
            throw new ConversationDeletedException();
        }
        conversation.rename(ConversationTitle.of(command.newTitle()));
        conversationRepository.save(conversation);
    }

    @Override
    public Conversation addMessage(AddMessageCommand command) {
        Conversation conversation = load(ConversationId.of(command.conversationId()));

        MessageId messageId = idFor("message", command.idempotencyKey())
                .map(MessageId::of)
                .orElseGet(() -> MessageId.of(UUID.randomUUID()));

        // Replay: the same Idempotency-Key yields the same message id, so a retry returns
        // the conversation as-is (even if it was since archived) rather than re-appending.
        if (command.idempotencyKey() != null
                && conversation.getMessages().stream().anyMatch(m -> m.getId().equals(messageId))) {
            return conversation;
        }

        ensureAcceptsMessages(conversation);

        Message message = new Message(
                messageId,
                MessageContent.of(command.content()),
                MessageRole.valueOf(command.role()),
                TokenUsage.of(command.promptTokens(), command.completionTokens())
        );
        conversation.addMessage(message); // no-op in the aggregate if the id is already present

        try {
            return conversationRepository.save(conversation);
        } catch (OptimisticLockingFailureException concurrentRetry) {
            // A concurrent retry appended this message first; return the committed state.
            return load(ConversationId.of(command.conversationId()));
        }
    }

    @Override
    public void archive(UUID conversationId) {
        Conversation conversation = load(ConversationId.of(conversationId));
        switch (conversation.getStatus()) {
            case DELETED -> throw new ConversationDeletedException();
            case ARCHIVED -> throw new ConversationAlreadyArchivedException();
            case ACTIVE -> conversation.archive();
        }
        conversationRepository.save(conversation);
    }

    @Override
    public void delete(UUID conversationId) {
        Conversation conversation = load(ConversationId.of(conversationId));
        conversation.delete();
        conversationRepository.save(conversation);
    }

    private Conversation load(ConversationId id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> ConversationNotFoundException.of(id.toString()));
    }

    /**
     * Maps a present, non-blank Idempotency-Key to a stable UUID for the given scope, so a
     * retry resolves to the same entity id. The scope keeps a key used for a conversation and
     * for a message from colliding. Empty when no key was supplied (ids stay random).
     */
    private static Optional<UUID> idFor(String scope, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.nameUUIDFromBytes(
                (scope + ':' + idempotencyKey).getBytes(StandardCharsets.UTF_8)));
    }

    /** Only ACTIVE conversations accept new turns; other states map to explicit domain errors. */
    private void ensureAcceptsMessages(Conversation conversation) {
        switch (conversation.getStatus()) {
            case DELETED -> throw new ConversationDeletedException();
            case ARCHIVED -> throw new InvalidConversationStateException(
                    "Cannot add a message to an archived conversation.");
            case ACTIVE -> { /* ok */ }
        }
    }
}
