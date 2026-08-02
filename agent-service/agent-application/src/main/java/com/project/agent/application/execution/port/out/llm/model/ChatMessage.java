package com.project.agent.application.execution.port.out.llm.model;

import com.project.agent.domain.message.MessageRole;

/**
 * Lightweight transport record for a single message sent to the model — free of
 * persistence identity and of any langchain4j type. The adapter converts this to
 * the provider's native message type.
 */
public record ChatMessage(
        MessageRole role,
        String content
) {}
