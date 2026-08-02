package com.project.agent.application.execution.service.streaming;

import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.in.usecase.StreamAgentUseCase;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.application.execution.service.workflow.AgentExecutionWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Streaming implementation of {@link com.project.agent.application.execution.port.in.usecase.StreamAgentUseCase}.
 * Prepares the execution via {@link AgentExecutionWorkflow}, streams the LLM response
 * through {@link StreamingLlmInvocationService}, finalises the execution on completion and
 * forwards every streamed event to the caller's handler.
 */
@Service
@RequiredArgsConstructor
public class AgentStreamingService implements StreamAgentUseCase {

    private final AgentExecutionWorkflow workflow;

    private final StreamingLlmInvocationService streamingLlmInvocationService;

    @Override
    public void stream(
            RunAgentCommand command,
            StreamingChatHandler handler
    ) {

        AgentExecutionContext context =
                workflow.prepare(command);

        streamingLlmInvocationService.stream(
                context,
                new StreamingChatHandler() {

                    @Override
                    public void onToken(String token) {
                        handler.onToken(token);
                    }

                    @Override
                    public void onToolCall(ToolCall toolCall) {
                        handler.onToolCall(toolCall);
                    }

                    @Override
                    public void onComplete(ChatResult result) {
                        workflow.finish(context, result);
                        handler.onComplete(result);
                    }

                    @Override
                    public void onError(Throwable error) {
                        handler.onError(error);
                    }
                }
        );
    }
}
