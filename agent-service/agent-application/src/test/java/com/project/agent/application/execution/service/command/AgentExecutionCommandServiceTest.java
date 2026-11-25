package com.project.agent.application.execution.service.command;


import com.project.agent.application.execution.exception.IdempotencyConflictException;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.application.execution.port.out.idempotency.IdempotencyStorePort;
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
import java.time.Duration;
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

    @Mock
    private AgentExecutionRepositoryPort agentExecutionRepository;

    private FakeIdempotencyStore idempotencyStore;

    private AgentExecutionCommandService service;

    @BeforeEach
    void setUp() {
        idempotencyStore = new FakeIdempotencyStore();
        service = new AgentExecutionCommandService(
                workflow,
                llmInvocationService,
                idempotencyStore,
                agentExecutionRepository
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
                        List.of(),
                        null
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
                        List.of(),
                        null
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
                        List.of(),
                        null
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

    @Test
    void run_withIdempotencyKey_replaysOnRetry() {

        AgentExecutionContext context =
                AgentExecutionContextBuilder.aContext().build();

        AgentExecution execution =
                AgentExecutionBuilder.anExecution().build();

        ChatResult result = new ChatResult(
                "Hello!",
                TokenUsage.of(12, 8),
                Money.of(BigDecimal.ONE, Currency.getInstance("USD")),
                List.of()
        );

        RunAgentCommand command =
                new RunAgentCommand(
                        UUID.randomUUID(),
                        "Hi",
                        "gpt-4o",
                        "openai",
                        List.of(),
                        "key-1"
                );

        when(workflow.prepare(command)).thenReturn(context);
        when(llmInvocationService.invoke(any(), any(), any(), any(), any())).thenReturn(result);
        when(workflow.finish(context, result)).thenReturn(execution);
        when(agentExecutionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));

        AgentExecution first = service.run(command);
        AgentExecution replay = service.run(command); // retry with the same key

        assertSame(execution, first);
        assertSame(execution, replay);
        // The expensive path (LLM + persistence) ran exactly once; the retry replayed.
        verify(workflow, times(1)).prepare(command);
        verify(llmInvocationService, times(1)).invoke(any(), any(), any(), any(), any());
    }

    @Test
    void run_withKeyAlreadyInFlight_throwsConflict() {

        IdempotencyStorePort store = mock(IdempotencyStorePort.class);
        AgentExecutionCommandService svc = new AgentExecutionCommandService(
                workflow, llmInvocationService, store, agentExecutionRepository);

        RunAgentCommand command =
                new RunAgentCommand(
                        UUID.randomUUID(),
                        "Hi",
                        "gpt-4o",
                        "openai",
                        List.of(),
                        "key-busy"
                );

        when(store.find("key-busy")).thenReturn(Optional.of(
                new IdempotencyStorePort.Entry(
                        "key-busy", "other-fingerprint", IdempotencyStorePort.Status.IN_PROGRESS, null)));

        assertThrows(IdempotencyConflictException.class, () -> svc.run(command));
        verifyNoInteractions(workflow, llmInvocationService);
    }

    /** In-memory {@link IdempotencyStorePort} so the service can be exercised without a database. */
    private static final class FakeIdempotencyStore implements IdempotencyStorePort {

        private final Map<String, Entry> entries = new HashMap<>();

        @Override
        public Optional<Entry> find(String key) {
            return Optional.ofNullable(entries.get(key));
        }

        @Override
        public boolean reserve(String key, String fingerprint, Duration ttl) {
            if (entries.containsKey(key)) {
                return false;
            }
            entries.put(key, new Entry(key, fingerprint, Status.IN_PROGRESS, null));
            return true;
        }

        @Override
        public void complete(String key, UUID resultId) {
            Entry current = entries.get(key);
            entries.put(key, new Entry(key, current.fingerprint(), Status.COMPLETED, resultId));
        }

        @Override
        public void release(String key) {
            entries.remove(key);
        }
    }
}
