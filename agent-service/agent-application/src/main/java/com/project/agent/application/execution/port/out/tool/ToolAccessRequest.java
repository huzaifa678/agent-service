package com.project.agent.application.execution.port.out.tool;

import java.util.Map;

/**
 * A single tool invocation to be adjudicated by the policy engine. This is the {@code input}
 * document handed to OPA — deliberately a plain, serialisable shape with no domain or
 * framework types, so the application core stays agnostic of how the decision is made.
 *
 * @param toolName    the tool the agent wants to run (e.g. {@code web_search})
 * @param arguments   the raw request/arguments payload for the call, or {@code ""} when the
 *                    tool is merely being enabled pre-flight rather than invoked
 * @param attributes  free-form hints the policy may key on (e.g. a tenant id or entitlement
 *                    tier once threaded end-to-end); never {@code null}
 */
public record ToolAccessRequest(
        String toolName,
        String arguments,
        Map<String, Object> attributes
) {

    public ToolAccessRequest {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    /** An authorization check for a tool the client wants enabled, before any invocation. */
    public static ToolAccessRequest forEnablement(String toolName, Map<String, Object> attributes) {
        return new ToolAccessRequest(toolName, "", attributes);
    }

    /** An authorization check for a concrete tool call the model asked to make. */
    public static ToolAccessRequest forInvocation(String toolName, String arguments, Map<String, Object> attributes) {
        return new ToolAccessRequest(toolName, arguments, attributes);
    }
}
