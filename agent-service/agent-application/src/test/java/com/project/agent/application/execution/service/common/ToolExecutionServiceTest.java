package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.application.execution.port.out.tool.ToolExecutorPort;
import com.project.agent.application.execution.port.out.tool.ToolResult;
import com.project.agent.application.execution.service.common.builder.AgentExecutionBuilder;
import com.project.agent.application.execution.service.harness.AgentToolHarness;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.tool.ToolExecutionStatus;
import com.project.agent.domain.execution.tool.exception.ToolInvocationException;
import com.project.agent.domain.execution.tool.exception.ToolNotAuthorizedException;
import com.project.agent.domain.execution.tool.exception.UnsupportedToolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolExecutionServiceTest {

    @Mock
    private ToolExecutorPort toolExecutor;

    @Mock
    private AgentToolHarness toolHarness;

    @InjectMocks
    private ToolExecutionService service;

    private AgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = AgentExecutionBuilder.anExecution().build();
        // Default to an all-allowing tool policy; individual tests override to exercise denial.
        lenient().when(toolHarness.authorizeEnablement(anyString())).thenReturn(true);
        lenient().when(toolHarness.authorizeInvocation(anyString(), any())).thenReturn(true);
    }

    @Test
    void execute_withNoToolCalls_doesNothing() {

        assertDoesNotThrow(() ->
                service.execute(execution, List.of())
        );

        assertEquals(0, execution.totalToolExecutions());

        verifyNoInteractions(toolExecutor);
    }

    @Test
    void validateTools_null_returnsEmptyList() {

        List<String> tools = service.validateTools(null);

        assertEquals(List.of(), tools);
    }

    @Test
    void validateTools_unsupportedTool_throwsException() {

        assertThrows(
                UnsupportedToolException.class,
                () -> service.validateTools(
                        List.of("shell")
                )
        );
    }

    @Test
    void validateTools_unauthorizedTool_throwsException() {

        when(toolHarness.authorizeEnablement("code_interpreter")).thenReturn(false);

        assertThrows(
                ToolNotAuthorizedException.class,
                () -> service.validateTools(
                        List.of("code_interpreter")
                )
        );
    }

    @Test
    void execute_policyDeniesInvocation_recordsFailedExecution_andSkipsExecutor() {

        ToolCall call = new ToolCall(
                "web_search",
                "{\"url\":\"http://localhost/admin\"}"
        );

        when(toolHarness.authorizeInvocation("web_search", call.request()))
                .thenReturn(false);

        service.execute(
                execution,
                List.of(call)
        );

        assertEquals(1, execution.totalToolExecutions());
        assertEquals(
                ToolExecutionStatus.FAILED,
                execution.getToolExecutions()
                        .getFirst()
                        .getStatus()
        );
        verifyNoInteractions(toolExecutor);
    }

    @Test
    void execute_success_recordsCompletedToolExecution() {

        ToolCall call = new ToolCall(
                "calculator",
                "{\"expression\":\"2+2\"}"
        );

        when(toolExecutor.execute(call))
                .thenReturn(
                        new ToolResult("4")
                );

        service.execute(
                execution,
                List.of(call)
        );

        assertEquals(1, execution.totalToolExecutions());

        assertEquals(
                ToolExecutionStatus.COMPLETED,
                execution.getToolExecutions()
                        .getFirst()
                        .getStatus()
        );
    }

    @Test
    void execute_toolInvocationFailure_recordsFailedExecution() {

        ToolCall call = new ToolCall(
                "calculator",
                "{\"expression\":\"2+2\"}"
        );

        when(toolExecutor.execute(call))
                .thenThrow(
                        new ToolInvocationException("calculator")
                );

        service.execute(
                execution,
                List.of(call)
        );

        assertEquals(1, execution.totalToolExecutions());

        assertEquals(
                ToolExecutionStatus.FAILED,
                execution.getToolExecutions()
                        .getFirst()
                        .getStatus()
        );
    }

    @Test
    void execute_invalidToolResponse_recordsFailedExecution() {

        ToolCall call = new ToolCall(
                "calculator",
                "{}"
        );

        when(toolExecutor.execute(call))
                .thenReturn(
                        new ToolResult("")
                );

        service.execute(
                execution,
                List.of(call)
        );

        assertEquals(1, execution.totalToolExecutions());

        assertEquals(
                ToolExecutionStatus.FAILED,
                execution.getToolExecutions()
                        .getFirst()
                        .getStatus()
        );
    }
}
