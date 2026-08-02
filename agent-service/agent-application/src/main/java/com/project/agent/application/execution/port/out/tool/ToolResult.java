package com.project.agent.application.execution.port.out.tool;

/** The response payload returned by a tool invocation. */
public record ToolResult(
        String response
) {}
