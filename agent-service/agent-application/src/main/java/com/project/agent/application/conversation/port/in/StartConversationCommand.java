package com.project.agent.application.conversation.port.in;

import java.util.UUID;

/**
 * Command to open a new conversation for a user within a tenant.
 *
 * <p>{@code idempotencyKey} is optional (the forwarded {@code Idempotency-Key} header):
 * when present it makes the conversation id deterministic, so a retried start returns the
 * original conversation instead of creating a second one.
 */
public record StartConversationCommand(
        UUID tenantId,
        UUID userId,
        String title,
        String idempotencyKey
) {}
