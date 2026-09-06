package com.project.agent.adapter.in.rest.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SinkStreamingHandlerTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    @Test
    void emitsTokensThenCompletedEvent() {

        Sinks.Many<ServerSentEvent<Object>> sink =
                Sinks.many().unicast().onBackpressureBuffer();

        SinkStreamingHandler handler = new SinkStreamingHandler(sink);

        handler.onToken("he");
        handler.onToken("llo");
        handler.onComplete(null);

        List<ServerSentEvent<Object>> events =
                sink.asFlux().collectList().block(TIMEOUT);

        assertThat(events).hasSize(3);
        assertThat(events.get(0).event()).isEqualTo("token");
        assertThat(events.get(0).data()).isEqualTo("he");
        assertThat(events.get(1).data()).isEqualTo("llo");
        assertThat(events.get(2).event()).isEqualTo("completed");
    }

    @Test
    void onError_terminatesFluxWithError() {

        Sinks.Many<ServerSentEvent<Object>> sink =
                Sinks.many().unicast().onBackpressureBuffer();

        SinkStreamingHandler handler = new SinkStreamingHandler(sink);

        handler.onError(new RuntimeException("boom"));

        assertThatThrownBy(() -> sink.asFlux().collectList().block(TIMEOUT))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }
}
