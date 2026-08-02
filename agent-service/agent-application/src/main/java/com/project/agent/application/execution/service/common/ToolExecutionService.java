package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.application.execution.port.out.tool.ToolExecutorPort;
import com.project.agent.application.execution.port.out.tool.ToolResult;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.tool.ToolExecution;
import com.project.agent.domain.execution.tool.exception.InvalidToolResponseException;
import com.project.agent.domain.execution.tool.exception.ToolExecutionAlreadyCompletedException;
import com.project.agent.domain.execution.tool.exception.ToolInvocationException;
import com.project.agent.domain.execution.tool.exception.ToolTimeoutException;
import com.project.agent.domain.execution.tool.exception.UnsupportedToolException;
import com.project.agent.domain.vo.ai.ToolName;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ToolExecutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Executes all tool invocations requested by the language model.
 *
 * <p>This service validates requested tools, delegates execution to the
 * configured tool executor, and records the outcome on the owning
 * {@link AgentExecution}. Tool failures are isolated from the overall
 * agent execution and only affect the corresponding
 * {@link ToolExecution}.
 *
 * <p>Supported tool names are currently hard-coded placeholders and
 * should eventually be loaded from configuration.
 */
@Service
@RequiredArgsConstructor
public class ToolExecutionService {

    // TODO Move to configuration.

    private static final Set<String> SUPPORTED_TOOLS =
            Set.of(
                    "web_search",
                    "calculator",
                    "code_interpreter"
            );

    private final ToolExecutorPort toolExecutor;

    /**
     * Executes every tool requested by the language model.
     */
    public void execute(
            AgentExecution execution,
            List<ToolCall> toolCalls
    ) {

        if (toolCalls == null || toolCalls.isEmpty()) {
            return;
        }

        for (ToolCall call : toolCalls) {
            handleToolCall(execution, call);
        }
    }

    /**
     * Validates all enabled tools supplied by the client before an
     * LLM request is made.
     */
    public List<String> validateTools(List<String> tools) {

        if (tools == null) {
            return List.of();
        }

        tools.forEach(this::validateTool);

        return tools;
    }

    private void handleToolCall(
            AgentExecution execution,
            ToolCall call
    ) {

        validateTool(call.toolName());

        ToolExecution toolExecution = ToolExecution.start(
                ToolExecutionId.of(UUID.randomUUID()),
                ToolName.of(call.toolName()),
                MessageContent.of(call.request())
        );

        try {

            ToolResult result = toolExecutor.execute(call);

            if (result == null
                    || result.response() == null
                    || result.response().isBlank()) {

                throw new InvalidToolResponseException(
                        call.toolName()
                );
            }

            complete(toolExecution, result.response());

        } catch (
                ToolTimeoutException
                | ToolInvocationException
                | InvalidToolResponseException e
        ) {

            fail(toolExecution);
        }

        execution.addToolExecution(toolExecution);
    }

    private void validateTool(String toolName) {

        if (!SUPPORTED_TOOLS.contains(toolName)) {
            throw new UnsupportedToolException(toolName);
        }
    }

    private void complete(
            ToolExecution execution,
            String response
    ) {

        try {

            execution.complete(
                    MessageContent.of(response)
            );

        } catch (IllegalStateException e) {

            throw new ToolExecutionAlreadyCompletedException();
        }
    }

    private void fail(ToolExecution execution) {

        try {

            execution.fail();

        } catch (IllegalStateException e) {

            throw new ToolExecutionAlreadyCompletedException();
        }
    }
}
