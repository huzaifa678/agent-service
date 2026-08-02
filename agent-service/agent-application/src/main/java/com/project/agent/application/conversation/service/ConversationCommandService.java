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
import com.project.agent.application.shared.port.out.DomainEventPublisherPort;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final DomainEventPublisherPort eventPublisher;

    @Override
    public Conversation start(StartConversationCommand command) {
        Conversation conversation = new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(command.tenantId()),
                UserId.of(command.userId()),
                ConversationTitle.of(command.title())
        );
        Conversation saved = conversationRepository.save(conversation);
        publishAndClear(conversation);
        return saved;
    }

    @Override
    public void rename(RenameConversationCommand command) {
        Conversation conversation = load(ConversationId.of(command.conversationId()));
        if (conversation.getStatus() == com.project.agent.domain.conversation.ConversationStatus.DELETED) {
            throw new ConversationDeletedException();
        }
        conversation.rename(ConversationTitle.of(command.newTitle()));
        conversationRepository.save(conversation);
        publishAndClear(conversation);
    }

    @Override
    public Conversation addMessage(AddMessageCommand command) {
        Conversation conversation = load(ConversationId.of(command.conversationId()));
        ensureAcceptsMessages(conversation);

        Message message = new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of(command.content()),
                MessageRole.valueOf(command.role()),
                TokenUsage.of(command.promptTokens(), command.completionTokens())
        );
        conversation.addMessage(message);

        Conversation saved = conversationRepository.save(conversation);
        publishAndClear(conversation);
        return saved;
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
        publishAndClear(conversation);
    }

    @Override
    public void delete(UUID conversationId) {
        Conversation conversation = load(ConversationId.of(conversationId));
        conversation.delete();
        conversationRepository.save(conversation);
        publishAndClear(conversation);
    }

    private Conversation load(ConversationId id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> ConversationNotFoundException.of(id.toString()));
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

    private void publishAndClear(Conversation conversation) {
        eventPublisher.publishAll(conversation.domainEvents());
        conversation.clearDomainEvents();
    }
}
