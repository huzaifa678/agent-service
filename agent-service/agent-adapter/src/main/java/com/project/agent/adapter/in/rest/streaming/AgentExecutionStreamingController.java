package com.project.agent.adapter.in.rest.streaming;

import com.project.agent.adapter.in.rest.dto.RunAgentRequest;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.in.usecase.StreamAgentUseCase;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * REST adapter for the streaming agent-execution endpoint under
 * {@code /api/v1/agent/executions/stream}. Returns a Reactor {@link Flux} of
 * Server-Sent Events (tokens, tool calls, completion); Spring MVC adapts the reactive
 * return type over async servlet, so this stays on the servlet stack (with virtual
 * threads) without pulling in WebFlux.
 *
 * <p>The application streaming use case is callback-based, so the controller bridges it
 * onto the {@code Flux} through a {@link Sinks.Many}: a {@link SinkStreamingHandler}
 * feeds each event into the sink. Preparation and the streaming call run on a bounded
 * elastic worker so the HTTP thread returns the {@code Flux} immediately; a failure
 * during preparation (e.g. an unknown conversation) is surfaced as a stream error.
 */
@RestController
@RequestMapping("/api/v1/agent/executions")
@RequiredArgsConstructor
public class AgentExecutionStreamingController {

    private final StreamAgentUseCase streamAgentUseCase;

    @PostMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<Object>> stream(
            @Valid @RequestBody RunAgentRequest request
    ) {

        Sinks.Many<ServerSentEvent<Object>> sink =
                Sinks.many().unicast().onBackpressureBuffer();

        StreamingChatHandler handler =
                new SinkStreamingHandler(sink);

        RunAgentCommand command =
                new RunAgentCommand(
                        request.conversationId(),
                        request.userMessage(),
                        request.modelName(),
                        request.providerName(),
                        request.enabledTools()
                );

        Mono.fromRunnable(() ->
                        streamAgentUseCase.stream(command, handler)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        unused -> {
                        },
                        sink::tryEmitError
                );

        return sink.asFlux();
    }
}
