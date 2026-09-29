package com.project.agent.application.conversation.service;

import com.project.agent.application.conversation.port.in.ConversationQueries;
import com.project.agent.application.conversation.port.out.ConversationReadModelPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.ConversationNotFoundException;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read side of the conversation context. Reads from the read-model projection
 * ({@link ConversationReadModelPort}) rather than replaying the event store, keeping queries
 * cheap while the write side remains event-sourced (CQRS).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConversationQueryService implements ConversationQueries {

    private final ConversationReadModelPort conversationReadModel;

    @Override
    public Conversation getById(UUID conversationId) {
        ConversationId id = ConversationId.of(conversationId);
        return conversationReadModel.findById(id)
                .orElseThrow(() -> ConversationNotFoundException.of(id.toString()));
    }

    @Override
    public List<Conversation> byUser(UUID userId) {
        return conversationReadModel.findByUserId(UserId.of(userId));
    }

    @Override
    public List<Message> messages(UUID conversationId) {
        return getById(conversationId).getMessages();
    }
}
