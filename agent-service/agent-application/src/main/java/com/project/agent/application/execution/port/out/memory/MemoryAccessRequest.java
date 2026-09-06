package com.project.agent.application.execution.port.out.memory;

import java.util.Map;

/**
 * A single memory access to be adjudicated by the policy engine. This is the {@code input}
 * document handed to OPA — deliberately a plain, serialisable shape with no domain or
 * framework types, so the application core stays agnostic of how the decision is made.
 *
 * @param operation   whether the agent is reading from or writing to memory
 * @param conversationId the conversation the memory belongs to
 * @param content     the passage being recalled, or the message being persisted
 * @param attributes  free-form hints the policy may key on (e.g. relevance score on a
 *                    recall, a tenant retention flag on a persist); never {@code null}
 */
public record MemoryAccessRequest(
        MemoryOperation operation,
        String conversationId,
        String content,
        Map<String, Object> attributes
) {

    public MemoryAccessRequest {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static MemoryAccessRequest recall(String conversationId, String content, Map<String, Object> attributes) {
        return new MemoryAccessRequest(MemoryOperation.RECALL, conversationId, content, attributes);
    }

    public static MemoryAccessRequest persist(String conversationId, String content, Map<String, Object> attributes) {
        return new MemoryAccessRequest(MemoryOperation.PERSIST, conversationId, content, attributes);
    }
}
