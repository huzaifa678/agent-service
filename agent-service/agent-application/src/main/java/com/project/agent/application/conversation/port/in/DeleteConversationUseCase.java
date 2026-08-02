package com.project.agent.application.conversation.port.in;

import java.util.UUID;

/** Inbound port: soft-delete a conversation. */
public interface DeleteConversationUseCase {

    void delete(UUID conversationId);
}
