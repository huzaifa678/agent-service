package com.project.agent.application.execution.port.out.tool;

/**
 * Outbound port for executing a tool/function call requested by the model.
 *
 * <p>Implementations signal failures with the domain tool exceptions:
 * {@link com.project.agent.domain.execution.tool.exception.ToolInvocationException},
 * {@link com.project.agent.domain.execution.tool.exception.ToolTimeoutException},
 * {@link com.project.agent.domain.execution.tool.exception.InvalidToolResponseException},
 * {@link com.project.agent.domain.execution.tool.exception.UnsupportedToolException}.
 */
public interface ToolExecutorPort {

    ToolResult execute(ToolCall call);
}
