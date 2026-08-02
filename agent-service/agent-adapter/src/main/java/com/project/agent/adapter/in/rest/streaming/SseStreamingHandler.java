package com.project.agent.adapter.in.rest.streaming;

import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Adapts the application's {@link com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler}
 * callbacks onto Server-Sent Events, forwarding each streamed token, tool call and
 * terminal signal to the client via {@link SseEventPublisher}.
 */
@RequiredArgsConstructor
public class SseStreamingHandler implements StreamingChatHandler {

    private final SseEmitter emitter;

    private final SseEventPublisher publisher;

    @Override
    public void onToken(String token) {
        publisher.token(emitter, token);
    }

    @Override
    public void onToolCall(ToolCall toolCall) {
        publisher.toolCall(emitter, toolCall);
    }

    @Override
    public void onComplete(ChatResult result) {
        publisher.completed(emitter);
    }

    @Override
    public void onError(Throwable throwable) {
        publisher.error(emitter, throwable);
    }
}
