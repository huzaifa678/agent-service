package com.project.agent.application.conversation.port.in;

import java.util.UUID;

/**
 * Command to append a single message to a conversation. {@code role} maps to
 * {@link com.project.agent.domain.message.MessageRole} (USER / ASSISTANT / SYSTEM / TOOL).
 */
public record AddMessageCommand(
        UUID conversationId,
        String role,
        String content,
        int promptTokens,
        int completionTokens
) {}
