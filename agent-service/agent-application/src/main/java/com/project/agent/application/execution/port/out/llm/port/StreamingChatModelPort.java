package com.project.agent.application.execution.port.out.llm.port;

import com.project.agent.application.execution.port.out.llm.model.ChatRequest;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;

/**
 * Outbound port for invoking an LLM in streaming mode. Implementations push tokens,
 * tool calls and the terminal result to the given
 * {@link com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler}.
 */
public interface StreamingChatModelPort {

    void stream(
            AgentExecutionContext context,
            StreamingChatHandler handler
    );
}
