package com.project.agent.application.execution.service.command;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.llm.port.ChatModelPort;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.service.command.commands.LlmInvocationService;
import com.project.agent.application.execution.service.common.builder.AgentExecutionBuilder;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.agent.AgentExecutionStatus;
import com.project.agent.domain.execution.agent.exception.AgentExecutionAlreadyCompletedException;
import com.project.agent.domain.execution.agent.exception.AgentExecutionFailedException;
import com.project.agent.domain.execution.agent.exception.ModelProviderException;
import com.project.agent.domain.execution.agent.exception.ModelTimeoutException;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmInvocationServiceTest {

    @Mock
    private ChatModelPort chatModel;

    @InjectMocks
    private LlmInvocationService service;

    @Test
    void invoke_success_returnsChatResult() {

        AgentExecution execution = AgentExecutionBuilder.anExecution().build();

        ChatResult expected = new ChatResult(
                "Hello",
                TokenUsage.of(10, 5),
                Money.zero(Currency.getInstance("USD")),
                List.of()
        );

        when(chatModel.generate(any())).thenReturn(expected);

        ChatResult actual = service.invoke(
                execution,
                ModelName.of("gpt-4o"),
                ProviderName.of("openai"),
                List.of(new ChatMessage(
                        com.project.agent.domain.message.MessageRole.USER,
                        "Hi"
                )),
                List.of()
        );

        assertEquals(expected, actual);

        verify(chatModel).generate(any());
    }

    @Test
    void invoke_timeout_marksExecutionTimedOut() {

        AgentExecution execution = AgentExecutionBuilder.anExecution().build();

        when(chatModel.generate(any()))
                .thenThrow(new ModelTimeoutException("gpt-4o"));

        assertThrows(
                AgentExecutionFailedException.class,
                () -> service.invoke(
                        execution,
                        ModelName.of("gpt-4o"),
                        ProviderName.of("openai"),
                        List.of(),
                        List.of()
                )
        );

        assertEquals(
                AgentExecutionStatus.TIMEOUT,
                execution.getStatus()
        );
    }

    @Test
    void invoke_providerFailure_marksExecutionFailed() {

        AgentExecution execution = AgentExecutionBuilder.anExecution().build();

        when(chatModel.generate(any()))
                .thenThrow(new ModelProviderException("provider failed"));

        assertThrows(
                AgentExecutionFailedException.class,
                () -> service.invoke(
                        execution,
                        ModelName.of("gpt-4o"),
                        ProviderName.of("openai"),
                        List.of(),
                        List.of()
                )
        );

        assertEquals(
                AgentExecutionStatus.FAILED,
                execution.getStatus()
        );
    }

    @Test
    void complete_marksExecutionCompleted() {

        AgentExecution execution = AgentExecutionBuilder.anExecution().build();

        ChatResult result = new ChatResult(
                "Hello",
                TokenUsage.of(12, 8),
                Money.zero(Currency.getInstance("USD")),
                List.of()
        );

        service.complete(
                execution,
                result
        );

        assertEquals(
                AgentExecutionStatus.COMPLETED,
                execution.getStatus()
        );

        assertEquals(
                result.tokenUsage(),
                execution.getTokenUsage()
        );

        assertEquals(
                result.cost(),
                execution.getCost()
        );
    }

    @Test
    void complete_alreadyCompleted_throwsException() {

        AgentExecution execution = AgentExecutionBuilder.anExecution().build();

        ChatResult result = new ChatResult(
                "Hello",
                TokenUsage.of(1, 1),
                Money.zero(Currency.getInstance("USD")),
                List.of()
        );

        service.complete(execution, result);

        assertThrows(
                AgentExecutionAlreadyCompletedException.class,
                () -> service.complete(execution, result)
        );
    }
}
