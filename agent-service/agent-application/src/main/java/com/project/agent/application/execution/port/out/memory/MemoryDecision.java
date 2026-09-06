package com.project.agent.application.execution.port.out.memory;

/**
 * The verdict for one {@link MemoryAccessRequest}: whether the access is permitted and a
 * short human-readable reason (surfaced in logs and metrics tags so a denial can be traced
 * back to the rule that produced it).
 */
public record MemoryDecision(
        boolean allowed,
        String reason
) {

    private static final MemoryDecision ALLOW = new MemoryDecision(true, "allowed");

    public static MemoryDecision allow() {
        return ALLOW;
    }

    public static MemoryDecision deny(String reason) {
        return new MemoryDecision(false, reason);
    }
}
