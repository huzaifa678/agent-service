package com.project.agent.adapter.out.llm;

import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.application.execution.port.out.tool.ToolExecutorPort;
import com.project.agent.application.execution.port.out.tool.ToolResult;
import com.project.agent.domain.execution.tool.exception.ToolInvocationException;
import org.springframework.stereotype.Component;

/**
 * Placeholder {@link ToolExecutorPort}. No concrete tools are wired yet, so every
 * call is reported as a (recoverable) invocation failure — the orchestrator marks
 * the individual {@code ToolExecution} FAILED and continues the turn.
 *
 * <p>TODO: replace with a real tool registry that dispatches by {@link ToolCall#toolName()}.
 */
@Component
public class ToolExecutorAdapter implements ToolExecutorPort {

    @Override
    public ToolResult execute(ToolCall call) {
        throw new ToolInvocationException(
                call.toolName(),
                new UnsupportedOperationException("No implementation registered for tool: " + call.toolName())
        );
    }
}
