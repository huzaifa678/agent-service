package com.project.agent.application.execution.port.out.llm.streaming;

import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.tool.ToolCall;

/**
 * Callback interface for consuming a streaming LLM response: {@link #onToken} per token,
 * {@link #onToolCall} per tool invocation, {@link #onComplete} on success and
 * {@link #onError} on failure.
 */
public interface StreamingChatHandler {

    void onToken(String token);

    void onToolCall(ToolCall toolCall);

    void onComplete(ChatResult result);

    void onError(Throwable throwable);

}
