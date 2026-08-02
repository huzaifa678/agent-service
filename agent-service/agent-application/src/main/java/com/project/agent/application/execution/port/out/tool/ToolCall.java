package com.project.agent.application.execution.port.out.tool;

/** A tool/function invocation requested by the model: the tool name and its JSON request payload. */
public record ToolCall(
        String toolName,
        String request
) {}
