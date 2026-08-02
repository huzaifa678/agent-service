package com.project.agent.adapter.in.rest.streaming;

import com.project.agent.adapter.in.rest.dto.RunAgentRequest;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.in.usecase.StreamAgentUseCase;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST adapter for the streaming agent-execution endpoint under
 * {@code /api/agent/executions/stream}. Returns Server-Sent Events (tokens, tool calls,
 * completion) through an {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter}.
 */
@RestController
@RequestMapping("/api/agent/executions")
@RequiredArgsConstructor
public class AgentExecutionStreamingController {

    private final StreamAgentUseCase streamAgentUseCase;
    private final SseEventPublisher publisher;

    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(
            @Valid @RequestBody RunAgentRequest request
    ) {
        SseEmitter emitter = new SseEmitter(0L);

        StreamingChatHandler handler =
                new SseStreamingHandler(
                        emitter,
                        publisher
                );

        streamAgentUseCase.stream(
                new RunAgentCommand(
                        request.conversationId(),
                        request.userMessage(),
                        request.modelName(),
                        request.providerName(),
                        request.enabledTools()
                ),
                handler
        );

        return emitter;
    }
}
