package com.project.agent.adapter.out.llm.mapper;


import com.project.agent.application.execution.port.out.llm.model.ChatRequest;
import com.project.agent.domain.execution.agent.exception.ModelAuthenticationException;
import com.project.agent.domain.execution.agent.exception.ModelProviderException;
import com.project.agent.domain.execution.agent.exception.ModelRateLimitException;
import com.project.agent.domain.execution.agent.exception.ModelTimeoutException;
import com.project.agent.domain.execution.agent.exception.ModelUnavailableException;
import com.project.agent.domain.execution.agent.exception.UnsupportedModelException;
import com.project.agent.domain.execution.agent.exception.prompt.ContextWindowExceededException;
import dev.langchain4j.exception.*;
import org.springframework.stereotype.Component;

/**
 * Translates LangChain4j/provider exceptions into the domain exceptions
 * understood by the application layer.
 *
 * <p>Some providers expose context-window violations only as generic
 * LangChain4j exceptions, so this mapper inspects the provider error
 * message to detect those cases.
 */
@Component
public class LangChainExceptionMapper {

    public RuntimeException translate(
            ChatRequest request,
            String providerLabel,
            RuntimeException exception
    ) {

        if (exception instanceof AuthenticationException e) {
            return new ModelAuthenticationException(
                    request.provider().value(),
                    e
            );
        }

        if (exception instanceof RateLimitException e) {
            return new ModelRateLimitException(
                    request.provider().value(),
                    e
            );
        }

        if (exception instanceof TimeoutException e) {
            return new ModelTimeoutException(
                    request.model().value(),
                    e
            );
        }

        if (exception instanceof ModelNotFoundException e) {
            return new UnsupportedModelException(
                    request.model().value(),
                    e
            );
        }

        if (exception instanceof InternalServerException e) {
            return new ModelUnavailableException(
                    request.provider().value(),
                    e
            );
        }

        if (exception instanceof LangChain4jException e) {

            if (isContextWindowExceeded(e)) {
                return new ContextWindowExceededException(
                        request.model().value(),
                        e
                );
            }

            return new ModelProviderException(
                    "LLM call failed via " + providerLabel + " provider",
                    e
            );
        }

        return exception;
    }

    /**
     * Attempts to detect provider responses indicating the prompt exceeded
     * the model context window.
     *
     * <p>LangChain4j currently does not expose a dedicated exception for
     * this condition across providers.
     */
    private boolean isContextWindowExceeded(Exception exception) {

        String message = exception.getMessage();

        if (message == null) {
            return false;
        }

        message = message.toLowerCase();

        return message.contains("maximum context length")
                || message.contains("context length exceeded")
                || message.contains("context window")
                || message.contains("too many tokens")
                || message.contains("prompt is too long")
                || message.contains("input is too long")
                || message.contains("token limit exceeded")
                || message.contains("context_length_exceeded");
    }
}
