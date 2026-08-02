package com.project.agent.application.execution.service.command.commands;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.llm.port.ChatModelPort;
import com.project.agent.application.execution.port.out.llm.model.ChatRequest;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.agent.exception.AgentExecutionAlreadyCompletedException;
import com.project.agent.domain.execution.agent.exception.AgentExecutionFailedException;
import com.project.agent.domain.execution.agent.exception.ModelAuthenticationException;
import com.project.agent.domain.execution.agent.exception.ModelProviderException;
import com.project.agent.domain.execution.agent.exception.ModelRateLimitException;
import com.project.agent.domain.execution.agent.exception.ModelTimeoutException;
import com.project.agent.domain.execution.agent.exception.ModelUnavailableException;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Executes a single Large Language Model request and manages the lifecycle
 * of the associated {@link AgentExecution}.
 *
 * <p>This service is responsible for invoking the configured LLM provider,
 * translating provider failures into domain-specific execution failures,
 * and updating the execution state when an invocation completes,
 * fails, or times out.
 */
@Service
@RequiredArgsConstructor
public class LlmInvocationService {

    private final ChatModelPort chatModel;

    /**
     * Executes the model request.
     *
     * @throws AgentExecutionFailedException if the provider cannot complete
     *         the request.
     */
    public ChatResult invoke(
            AgentExecution execution,
            ModelName model,
            ProviderName provider,
            List<ChatMessage> input,
            List<String> enabledTools
    ) {

        try {

            return chatModel.generate(
                    new ChatRequest(
                            model,
                            provider,
                            input,
                            enabledTools
                    )
            );

        } catch (ModelTimeoutException e) {

            timeout(execution);

            throw new AgentExecutionFailedException(
                    "LLM call timed out for model " + model,
                    e
            );

        } catch (
                ModelRateLimitException
                | ModelUnavailableException
                | ModelAuthenticationException
                | ModelProviderException e
        ) {

            fail(execution);

            throw new AgentExecutionFailedException(
                    "LLM call failed for provider " + provider,
                    e
            );
        }
    }

    /**
     * Marks an execution as successfully completed.
     */
    public void complete(
            AgentExecution execution,
            ChatResult result
    ) {

        try {

            execution.complete(
                    result.tokenUsage(),
                    result.cost()
            );

        } catch (IllegalStateException e) {

            throw new AgentExecutionAlreadyCompletedException();
        }
    }

    /**
     * Marks an execution as timed out.
     */
    private void timeout(AgentExecution execution) {

        try {

            execution.timeout();

        } catch (IllegalStateException e) {

            throw new AgentExecutionAlreadyCompletedException();
        }
    }

    /**
     * Marks an execution as failed.
     */
    private void fail(AgentExecution execution) {

        try {

            execution.fail();

        } catch (IllegalStateException e) {

            throw new AgentExecutionAlreadyCompletedException();
        }
    }
}
