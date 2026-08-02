package com.project.agent.adapter.out.llm;

import com.project.agent.adapter.out.llm.mapper.LangChainExceptionMapper;
import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.llm.port.ChatModelPort;
import com.project.agent.application.execution.port.out.llm.model.ChatRequest;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * langchain4j implementation of {@link ChatModelPort}. Calls are guarded by
 * Resilience4j: {@code @Retry} retries transient provider failures and
 * {@code @CircuitBreaker} trips after a sustained failure rate.
 *
 * <p>If the primary provider fails, the configured fallback attempts the
 * secondary provider. If both providers fail, the translated domain exception
 * is propagated so the application layer can mark the
 * {@code AgentExecution} as {@code FAILED}.
 *
 * <p>Provider SDK exceptions are translated into domain exceptions so the
 * application layer remains provider-agnostic.
 */
@Component
public class ChatModelAdapter implements ChatModelPort {

    private static final Logger log = LoggerFactory.getLogger(ChatModelAdapter.class);

    private static final Currency USD = Currency.getInstance("USD");

    // TODO Replace with configuration-driven per-model pricing.
    private static final BigDecimal USD_PER_TOKEN = new BigDecimal("0.000002");

    private final ChatModel primaryModel;
    private final ChatModel secondaryModel;
    private final LangChainExceptionMapper exceptionMapper;

    public ChatModelAdapter(
            @Qualifier("primaryChatModel") ChatModel primaryModel,
            @Qualifier("secondaryChatModel") ChatModel secondaryModel,
            LangChainExceptionMapper exceptionMapper
    ) {
        this.primaryModel = primaryModel;
        this.secondaryModel = secondaryModel;
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

            ChatResponse response = model.chat(
                    toLangChainMessages(request)
            );

            return toChatResult(response);

        } catch (RuntimeException exception) {

            throw exceptionMapper.translate(
                    request,
                    providerLabel,
                    exception
            );
        }
    }

    private List<dev.langchain4j.data.message.ChatMessage> toLangChainMessages(ChatRequest request) {

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();

        for (ChatMessage message : request.messages()) {

            String text = message.content();

            MessageRole role = message.role();

            switch (role) {

                case SYSTEM ->
                        messages.add(SystemMessage.from(text));

                case ASSISTANT ->
                        messages.add(AiMessage.from(text));

                // Tool results are currently folded into the user conversation.
                case USER, TOOL ->
                        messages.add(UserMessage.from(text));
            }
        }

        return messages;
    }

    private ChatResult toChatResult(ChatResponse response) {

        AiMessage aiMessage = response.aiMessage();

        String content =
                aiMessage.text() == null
                        ? ""
                        : aiMessage.text();

        dev.langchain4j.model.output.TokenUsage usage =
                response.tokenUsage();

        int inputTokens =
                usage != null && usage.inputTokenCount() != null
                        ? usage.inputTokenCount()
                        : 0;

        int outputTokens =
                usage != null && usage.outputTokenCount() != null
                        ? usage.outputTokenCount()
                        : 0;

        TokenUsage tokenUsage =
                TokenUsage.of(inputTokens, outputTokens);

        Money cost =
                estimateCost(tokenUsage);

        List<ToolCall> toolCalls = new ArrayList<>();

        if (aiMessage.hasToolExecutionRequests()) {

            aiMessage.toolExecutionRequests().forEach(request ->
                    toolCalls.add(
                            new ToolCall(
                                    request.name(),
                                    request.arguments()
                            )
                    )
            );
        }

        return new ChatResult(
                content,
                tokenUsage,
                cost,
                toolCalls
        );
    }

    private Money estimateCost(TokenUsage tokenUsage) {

        BigDecimal amount = USD_PER_TOKEN
                .multiply(
                        BigDecimal.valueOf(
                                tokenUsage.totalTokens()
                        )
                )
                .setScale(
                        4,
                        RoundingMode.HALF_UP
                );

        return Money.of(
                amount,
                USD
        );
    }

    /**
     * Attempts to detect provider responses indicating the prompt exceeded
     * the model's context window. LangChain4j currently exposes these as
     * generic provider exceptions rather than a dedicated exception type.
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
