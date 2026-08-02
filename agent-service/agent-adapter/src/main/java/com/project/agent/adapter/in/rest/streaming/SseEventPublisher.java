package com.project.agent.adapter.in.rest.streaming;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;


/**
 * Writes agent streaming events (token, tool_call, completed, error) to an
 * {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter} as named
 * Server-Sent Events.
 */
@Component
public class SseEventPublisher {

    public void token(
            SseEmitter emitter,
            String token
    ) {

        send(
                emitter,
                "token",
                token
        );
    }

    public void toolCall(
            SseEmitter emitter,
            Object tool
    ) {

        send(
                emitter,
                "tool_call",
                tool
        );
    }

    public void completed(
            SseEmitter emitter
    ) {

        send(
                emitter,
                "completed",
                null
        );

        emitter.complete();
    }

    public void error(
            SseEmitter emitter,
            Throwable error
    ) {

        emitter.completeWithError(error);
    }

    private void send(
            SseEmitter emitter,
            String event,
            Object data
    ) {

        try {

            emitter.send(
                    SseEmitter.event()
                            .name(event)
                            .data(data)
            );

        } catch (IOException e) {

            emitter.completeWithError(e);
        }
    }
}
