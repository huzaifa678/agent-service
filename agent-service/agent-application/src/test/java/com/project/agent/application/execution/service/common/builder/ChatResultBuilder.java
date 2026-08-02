package com.project.agent.application.execution.service.common.builder;

import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Test builder for {@link ChatResult}.
 */
public class ChatResultBuilder {

    private String content = "Assistant response";
    private TokenUsage tokenUsage = TokenUsage.of(10, 20);
    private Money cost = Money.of("0.0010", Currency.getInstance("USD"));
    private final List<ToolCall> toolCalls = new ArrayList<>();

    public static ChatResultBuilder aChatResult() {
        return new ChatResultBuilder();
    }

    public ChatResultBuilder withContent(String content) {
        this.content = content;
        return this;
    }

    public ChatResultBuilder withTokenUsage(TokenUsage usage) {
        this.tokenUsage = usage;
        return this;
    }

    public ChatResultBuilder withCost(Money cost) {
        this.cost = cost;
        return this;
    }

    public ChatResultBuilder withToolCall(ToolCall toolCall) {
        this.toolCalls.add(toolCall);
        return this;
    }

    public ChatResult build() {

        return new ChatResult(
                content,
                tokenUsage,
                cost,
                toolCalls
        );
    }
}
