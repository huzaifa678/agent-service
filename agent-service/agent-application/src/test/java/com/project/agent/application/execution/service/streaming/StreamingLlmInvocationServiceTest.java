package com.project.agent.application.execution.service.streaming;

import com.project.agent.application.execution.port.out.llm.port.StreamingChatModelPort;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.service.common.builder.AgentExecutionContextBuilder;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.domain.execution.agent.AgentExecutionStatus;
import com.project.agent.domain.execution.agent.exception.AgentExecutionFailedException;
import com.project.agent.domain.execution.agent.exception.ModelProviderException;
import com.project.agent.domain.execution.agent.exception.ModelTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StreamingLlmInvocationServiceTest {

    @Mock
    private StreamingChatModelPort chatModel;

    @Mock
    private StreamingChatHandler handler;

    private StreamingLlmInvocationService service;

    private AgentExecutionContext context;

    @BeforeEach
    void setUp() {

        service =
                new StreamingLlmInvocationService(
                        chatModel
                );

        context = AgentExecutionContextBuilder
                .aContext()
                .build();
    }

    @Test
    void stream_delegatesToChatModel() {

        service.stream(
                context,
                handler
        );

        verify(chatModel)
                .stream(
                        context,
                        handler
                );
    }

    @Test
    void stream_timeout_marksExecutionTimedOut() {

        AgentExecutionContext context =
                AgentExecutionContextBuilder
                        .aContext()
                        .build();

        doThrow(
                new ModelTimeoutException(
                        "gpt-4o"
                )
        ).when(chatModel)
                .stream(any(), any());

        assertThrows(
                AgentExecutionFailedException.class,
                () -> service.stream(
                        context,
                        handler
                )
        );

        assertEquals(
                AgentExecutionStatus.TIMEOUT,
                context.execution().getStatus()
        );
    }

    @Test
    void stream_providerFailure_marksExecutionFailed() {

        AgentExecutionContext context =
                AgentExecutionContextBuilder
                        .aContext()
                        .build();

        doThrow(
                new ModelProviderException("boom")
        ).when(chatModel)
                .stream(any(), any());

        assertThrows(
                AgentExecutionFailedException.class,
                () -> service.stream(
                        context,
                        handler
                )
        );

        assertEquals(
                AgentExecutionStatus.FAILED,
                context.execution().getStatus()
        );
    }
}
