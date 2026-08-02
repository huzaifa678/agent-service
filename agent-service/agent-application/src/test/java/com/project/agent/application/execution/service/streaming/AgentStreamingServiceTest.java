package com.project.agent.application.execution.service.streaming;

import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.service.common.builder.AgentExecutionContextBuilder;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.application.execution.service.workflow.AgentExecutionWorkflow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentStreamingServiceTest {

    @Mock
    private AgentExecutionWorkflow workflow;

    @Mock
    private StreamingLlmInvocationService streamingService;

    private AgentStreamingService service;

    private AgentExecutionContext context;

    @BeforeEach
    void setUp() {

        service =
                new AgentStreamingService(
                        workflow,
                        streamingService
                );

        context =
                AgentExecutionContextBuilder
                        .aContext()
                        .build();
    }

    @Test
    void stream_preparesWorkflowAndStartsStreaming() {

        RunAgentCommand command =
                new RunAgentCommand(
                        UUID.randomUUID(),
                        "Hello",
                        "gpt-4o",
                        "openai",
                        new ArrayList<>()
                );

        StreamingChatHandler handler =
                mock(StreamingChatHandler.class);

        when(workflow.prepare(any(RunAgentCommand.class)))
                .thenReturn(context);

        // Act
        service.stream(command, handler);

        // Assert
        verify(workflow)
                .prepare(command);

        verify(streamingService)
                .stream(
                        eq(context),
                        any(StreamingChatHandler.class)
                );
    }
}
