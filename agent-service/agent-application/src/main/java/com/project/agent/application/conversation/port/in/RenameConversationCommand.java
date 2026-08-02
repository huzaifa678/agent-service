package com.project.agent.application.conversation.port.in;

import java.util.UUID;

/** Command to change a conversation's title. */
public record RenameConversationCommand(
        UUID conversationId,
        String newTitle
) {}
