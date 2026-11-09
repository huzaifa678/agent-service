package com.project.agent.application.execution.port.out.guardrail;

import java.util.Map;

/**
 * A single input/output check to be adjudicated by the guardrail policy engine. This is the
 * {@code input} document handed to OPA — a plain, serialisable shape with no domain or
 * framework types, so the application core stays agnostic of how the decision is made.
 *
 * @param direction      whether this is the inbound user message or the outbound answer
 * @param conversationId the conversation the content belongs to
 * @param content        the user message (INPUT) or the assistant answer (OUTPUT)
 * @param attributes     free-form hints the policy may key on — notably
 *                       {@code grounding_confidence} on an OUTPUT check, so the policy can
 *                       enforce a minimum-grounding floor; never {@code null}
 */
public record GuardrailRequest(
        GuardrailDirection direction,
        String conversationId,
        String content,
        Map<String, Object> attributes
) {

    public GuardrailRequest {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static GuardrailRequest input(String conversationId, String content, Map<String, Object> attributes) {
        return new GuardrailRequest(GuardrailDirection.INPUT, conversationId, content, attributes);
    }

    public static GuardrailRequest output(String conversationId, String content, Map<String, Object> attributes) {
        return new GuardrailRequest(GuardrailDirection.OUTPUT, conversationId, content, attributes);
    }
}
