package com.project.agent.application.execution.port.out.tool;

/**
 * The verdict for one {@link ToolAccessRequest}: whether the tool call is permitted and a
 * short human-readable reason (surfaced in logs and metrics so a denial can be traced back to
 * the rule that produced it).
 */
public record ToolDecision(
        boolean allowed,
        String reason
) {

    private static final ToolDecision ALLOW = new ToolDecision(true, "allowed");

    public static ToolDecision allow() {
        return ALLOW;
    }

    public static ToolDecision deny(String reason) {
        return new ToolDecision(false, reason);
    }
}
