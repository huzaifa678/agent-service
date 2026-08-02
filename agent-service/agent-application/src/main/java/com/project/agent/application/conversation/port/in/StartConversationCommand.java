package com.project.agent.application.conversation.port.in;

import java.util.UUID;

/** Command to open a new conversation for a user within a tenant. */
public record StartConversationCommand(
        UUID tenantId,
        UUID userId,
        String title
) {}
