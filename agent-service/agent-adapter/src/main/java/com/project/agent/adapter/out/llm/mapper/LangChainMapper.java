package com.project.agent.adapter.out.llm.mapper;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Translates between the application's provider-agnostic chat types and langchain4j.
 * Shared by the synchronous {@link com.project.agent.adapter.out.llm.ChatModelAdapter}
 * and the streaming
 * {@link com.project.agent.adapter.out.llm.LangChainStreamingChatModelAdapter} so both
 * paths map messages, token usage and cost identically.
 */
@Component
public class LangChainMapper {

    private static final Currency USD = Currency.getInstance("USD");

    // TODO Replace with configuration-driven per-model pricing.
    private static final BigDecimal USD_PER_TOKEN = new BigDecimal("0.000002");

    /** Maps the application's chat messages onto langchain4j message types. */
    public List<dev.langchain4j.data.message.ChatMessage> toLangChainMessages(
            List<ChatMessage> messages
    ) {

        List<dev.langchain4j.data.message.ChatMessage> mapped = new ArrayList<>();

        for (ChatMessage message : messages) {

            String text = message.content();

            MessageRole role = message.role();

            switch (role) {

                case SYSTEM ->
                        mapped.add(SystemMessage.from(text));

                case ASSISTANT ->
                        mapped.add(AiMessage.from(text));

                // Tool results are currently folded into the user conversation.
                case USER, TOOL ->
                        mapped.add(UserMessage.from(text));
            }
        }

        return mapped;
    }

    /** Maps a langchain4j response onto the application's {@link ChatResult}. */
    public ChatResult toChatResult(ChatResponse response) {

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
}
