package com.project.agent.adapter.out.llm;

import com.project.agent.adapter.out.llm.mapper.LangChainExceptionMapper;
import com.project.agent.adapter.out.llm.mapper.LangChainMapper;
import com.project.agent.adapter.out.llm.schema.AgentResponseSchema;
import com.project.agent.application.execution.port.out.llm.model.ChatRequest;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.llm.port.ChatModelPort;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * langchain4j implementation of {@link ChatModelPort}. Calls are guarded by
 * Resilience4j: {@code @Retry} retries transient provider failures and
 * {@code @CircuitBreaker} trips after a sustained failure rate.
 *
 * <p>Every request carries the shared {@link AgentResponseSchema} response format,
 * so the model returns a JSON object validated against the agent-response schema
 * (strict, server-side, on providers that support it).
 *
 * <p>If the primary provider fails, the configured fallback attempts the
 * secondary provider. If both providers fail, the translated domain exception
 * is propagated so the application layer can mark the
 * {@code AgentExecution} as {@code FAILED}.
 */
@Component
public class ChatModelAdapter implements ChatModelPort {

    private static final Logger log = LoggerFactory.getLogger(ChatModelAdapter.class);

    private final ChatModel primaryModel;
    private final ChatModel secondaryModel;
    private final LangChainMapper mapper;
    private final LangChainExceptionMapper exceptionMapper;

    public ChatModelAdapter(
            @Qualifier("primaryChatModel") ChatModel primaryModel,
            @Qualifier("secondaryChatModel") ChatModel secondaryModel,
            LangChainMapper mapper,
            LangChainExceptionMapper exceptionMapper
    ) {
        this.primaryModel = primaryModel;
        this.secondaryModel = secondaryModel;
        this.mapper = mapper;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    @Retry(name = "llmProvider", fallbackMethod = "generateFallback")
    @CircuitBreaker(name = "llmProvider")
    public ChatResult generate(ChatRequest request) {
        return invoke(primaryModel, request, "primary");
    }

    /**
     * Resilience4j fallback.
     *
     * <p>Attempts the secondary provider. If it also fails, propagates the
     * translated domain exception rather than returning a fake response.
     */
    public ChatResult generateFallback(
            ChatRequest request,
            Throwable cause
    ) {

        log.warn(
                "Primary LLM provider failed ({}): {}. Failing over to secondary.",
                cause.getClass().getSimpleName(),
                cause.getMessage()
        );

        try {
            return invoke(
                    secondaryModel,
                    request,
                    "secondary"
            );
        } catch (RuntimeException secondaryFailure) {

            log.error(
                    "Secondary LLM provider also failed.",
                    secondaryFailure
            );

            throw secondaryFailure;
        }
    }

    private ChatResult invoke(
            ChatModel model,
            ChatRequest request,
            String providerLabel
    ) {

        try {

            dev.langchain4j.model.chat.request.ChatRequest chatRequest =
                    dev.langchain4j.model.chat.request.ChatRequest.builder()
                            .messages(mapper.toLangChainMessages(request.messages()))
                            .responseFormat(AgentResponseSchema.responseFormat())
                            .build();

            ChatResponse response = model.chat(chatRequest);

            return mapper.toChatResult(response);

        } catch (RuntimeException exception) {

            throw exceptionMapper.translate(
                    request,
                    providerLabel,
                    exception
            );
        }
    }
}
