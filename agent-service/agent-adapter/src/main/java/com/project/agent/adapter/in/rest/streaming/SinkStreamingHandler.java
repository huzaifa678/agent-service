package com.project.agent.adapter.in.rest.streaming;

import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

/**
 * Bridges the application's callback-based {@link StreamingChatHandler} onto a Reactor
 * {@link Sinks.Many}, turning each streamed token, tool call and terminal signal into a
 * named {@link ServerSentEvent} that the controller exposes as a {@code Flux}.
 *
 * <p>Emissions arrive on the LLM provider's callback thread while the HTTP thread has
 * already returned the {@code Flux}, so failed emissions are tolerated rather than thrown
 * (the subscriber may have cancelled, e.g. the client disconnected).
 */
public class SinkStreamingHandler implements StreamingChatHandler {

    private final Sinks.Many<ServerSentEvent<Object>> sink;

    public SinkStreamingHandler(Sinks.Many<ServerSentEvent<Object>> sink) {
        this.sink = sink;
    }

    @Override
    public void onToken(String token) {
        sink.tryEmitNext(
                ServerSentEvent.builder()
                        .event("token")
                        .data(token)
                        .build()
        );
    }

    @Override
    public void onToolCall(ToolCall toolCall) {
        sink.tryEmitNext(
                ServerSentEvent.<Object>builder()
                        .event("tool_call")
                        .data(toolCall)
                        .build()
        );
    }

    @Override
    public void onComplete(ChatResult result) {
        sink.tryEmitNext(
                ServerSentEvent.builder()
                        .event("completed")
                        .data("")
                        .build()
        );
        sink.tryEmitComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        sink.tryEmitError(throwable);
    }
}
