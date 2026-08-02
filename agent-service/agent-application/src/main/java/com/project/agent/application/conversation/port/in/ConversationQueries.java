package com.project.agent.application.conversation.port.in;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.message.Message;

import java.util.List;
import java.util.UUID;

/** Inbound read port (light CQRS — reads through the same repository, no separate store). */
public interface ConversationQueries {

    Conversation getById(UUID conversationId);

    List<Conversation> byUser(UUID userId);

    List<Message> messages(UUID conversationId);
}
