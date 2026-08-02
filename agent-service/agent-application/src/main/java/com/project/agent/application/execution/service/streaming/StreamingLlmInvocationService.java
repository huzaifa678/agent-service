package com.project.agent.application.execution.service.streaming;

import com.project.agent.application.execution.port.out.llm.port.StreamingChatModelPort;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.domain.execution.agent.exception.*;
import com.project.agent.domain.execution.agent.exception.AgentExecutionAlreadyCompletedException;
import com.project.agent.domain.execution.agent.exception.AgentExecutionFailedException;
import com.project.agent.domain.execution.agent.exception.ModelAuthenticationException;
import com.project.agent.domain.execution.agent.exception.ModelProviderException;
import com.project.agent.domain.execution.agent.exception.ModelRateLimitException;
import com.project.agent.domain.execution.agent.exception.ModelTimeoutException;
import com.project.agent.domain.execution.agent.exception.ModelUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Wraps the streaming LLM invocation ({@link com.project.agent.application.execution.port.out.llm.port.StreamingChatModelPort})
 * with error handling, translating provider/model failures into
 * {@link com.project.agent.domain.execution.agent.exception.AgentExecutionFailedException}
 * and marking the execution timed-out or failed as appropriate.
 */
@Service
@RequiredArgsConstructor
public class StreamingLlmInvocationService implements StreamingChatModelPort {

    private final StreamingChatModelPort chatModel;

    public void stream(
            AgentExecutionContext context,
            StreamingChatHandler handler
    ) {

        try {

            chatModel.stream(
                    context,
                    handler
            );

        } catch (ModelTimeoutException e) {

            timeout(context);

            throw new AgentExecutionFailedException(
                    "Streaming request timed out.",
                    e
            );

        } catch (
                ModelRateLimitException
                | ModelUnavailableException
                | ModelAuthenticationException
                | ModelProviderException e
        ) {

            fail(context);

            throw new AgentExecutionFailedException(
                    "Streaming request failed.",
                    e
            );
        }
    }

    private void timeout(
            AgentExecutionContext context
    ) {

        try {

            context.execution().timeout();

        } catch (IllegalStateException e) {

            throw new AgentExecutionAlreadyCompletedException();
        }
    }

    private void fail(
            AgentExecutionContext context
    ) {

        try {

            context.execution().fail();

        } catch (IllegalStateException e) {

            throw new AgentExecutionAlreadyCompletedException();
        }
    }
}
