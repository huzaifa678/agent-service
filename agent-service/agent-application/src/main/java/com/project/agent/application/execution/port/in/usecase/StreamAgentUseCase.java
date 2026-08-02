package com.project.agent.application.execution.port.in.usecase;

import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;

/**
 * Inbound port for running an agent turn in streaming mode, delivering tokens, tool
 * calls and the final result to the supplied
 * {@link com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler}
 * as they are produced.
 */
public interface StreamAgentUseCase {
    void stream(
            RunAgentCommand command,
            StreamingChatHandler handler
    );
}
