package com.project.agent.adapter.in.rest.streaming;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class SseEventPublisherTest {

    private SseEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SseEventPublisher();
    }

    @Test
    void token_doesNotThrow() {

        assertDoesNotThrow(() ->
                publisher.token(
                        new SseEmitter(),
                        "hello"
                )
        );
    }

    @Test
    void toolCall_doesNotThrow() {

        assertDoesNotThrow(() ->
                publisher.toolCall(
                        new SseEmitter(),
                        "calculator"
                )
        );
    }

    @Test
    void completed_doesNotThrow() {

        assertDoesNotThrow(() ->
                publisher.completed(
                        new SseEmitter()
                )
        );
    }

    @Test
    void error_completesEmitter() {

        SseEmitter emitter =
                new SseEmitter();

        assertDoesNotThrow(() ->
                publisher.error(
                        emitter,
                        new RuntimeException("boom")
                )
        );
    }
}
