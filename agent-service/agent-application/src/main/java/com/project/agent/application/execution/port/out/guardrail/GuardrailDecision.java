package com.project.agent.application.execution.port.out.guardrail;

/**
 * The verdict for one {@link GuardrailRequest}: whether the content may pass the boundary and
 * a short human-readable reason (surfaced in logs and metrics so a block can be traced back to
 * the rule that produced it).
 */
public record GuardrailDecision(
        boolean allowed,
        String reason
) {

    private static final GuardrailDecision ALLOW = new GuardrailDecision(true, "allowed");

    public static GuardrailDecision allow() {
        return ALLOW;
    }

    public static GuardrailDecision deny(String reason) {
        return new GuardrailDecision(false, reason);
    }
}
