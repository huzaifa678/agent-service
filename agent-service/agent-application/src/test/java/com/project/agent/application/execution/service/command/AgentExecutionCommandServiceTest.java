package com.project.agent.application.execution.service.command;


import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.service.command.commands.*;
import com.project.agent.application.execution.service.common.*;
import com.project.agent.application.execution.service.common.builder.AgentExecutionBuilder;
import com.project.agent.application.execution.service.common.builder.AgentExecutionContextBuilder;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.application.execution.service.workflow.AgentExecutionWorkflow;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentExecutionCommandServiceTest {

    @Mock
    private AgentExecutionWorkflow workflow;

    @Mock
    private LlmInvocationService llmInvocationService;

    private AgentExecutionCommandService service;

    @BeforeEach
    void setUp() {
        service = new AgentExecutionCommandService(
                workflow,
                llmInvocationService
        );
    }

    @Test
    void run_successfullyExecutesAgentTurn() {

        AgentExecutionContext context =
                AgentExecutionContextBuilder
                        .aContext()
                        .build();

        AgentExecution execution =
                AgentExecutionBuilder
                        .anExecution()
                        .build();

        ChatResult result = new ChatResult(
                "Hello!",
                TokenUsage.of(12, 8),
                Money.of(
                        BigDecimal.ONE,
                        Currency.getInstance("USD")
                ),
                List.of()
        );

        RunAgentCommand command =
                new RunAgentCommand(
                        UUID.randomUUID(),
                        "Hi",
                        "gpt-4o",
                        "openai",
                        List.of()
                );

        when(workflow.prepare(command))
                .thenReturn(context);

        when(llmInvocationService.invoke(
                eq(context.execution()),
                eq(context.model()),
                eq(context.provider()),
                eq(context.prompt()),
                eq(context.enabledTools())
        )).thenReturn(result);

        when(workflow.finish(context, result))
                .thenReturn(execution);

        AgentExecution saved =
                service.run(command);

        assertSame(execution, saved);

        verify(workflow)
                .prepare(command);

        verify(llmInvocationService)
                .invoke(
                        eq(context.execution()),
                        eq(context.model()),
                        eq(context.provider()),
                        eq(context.prompt()),
                        eq(context.enabledTools())
                );

        verify(workflow)
                .finish(context, result);
    }

    @Test
    void run_prepareFails_propagatesException() {

        RunAgentCommand command =
                new RunAgentCommand(
                        UUID.randomUUID(),
                        "Hello",
                        "gpt-4o",
                        "openai",
                        List.of()
                );

        when(workflow.prepare(command))
                .thenThrow(new IllegalStateException());

        assertThrows(
                IllegalStateException.class,
                () -> service.run(command)
        );

        verifyNoInteractions(
                llmInvocationService
        );
    }

    @Test
    void run_llmInvocationFails_propagatesException() {

        AgentExecutionContext context =
                AgentExecutionContextBuilder
                        .aContext()
                        .build();

        RunAgentCommand command =
                new RunAgentCommand(
                        UUID.randomUUID(),
                        "Hello",
                        "gpt-4o",
                        "openai",
                        List.of()
                );

        when(workflow.prepare(command))
                .thenReturn(context);

        when(llmInvocationService.invoke(
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenThrow(
                new RuntimeException("LLM failed")
        );

        assertThrows(
                RuntimeException.class,
                () -> service.run(command)
        );

        verify(workflow)
                .prepare(command);

        verify(workflow, never())
                .finish(any(), any());
    }
}
