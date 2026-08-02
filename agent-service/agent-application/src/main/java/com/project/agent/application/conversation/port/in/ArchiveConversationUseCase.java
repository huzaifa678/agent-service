package com.project.agent.application.conversation.port.in;

import java.util.UUID;

/** Inbound port: archive (make read-only) a conversation. */
public interface ArchiveConversationUseCase {

    void archive(UUID conversationId);
}
