package com.project.agent.application.execution.service.streaming;

import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable accumulator that assembles a streamed LLM response - content deltas, tool calls,
 * token usage and cost - into a final
 * {@link com.project.agent.application.execution.port.out.llm.model.ChatResult}.
 */
public class StreamingResponseAssembler {

    private final StringBuilder content = new StringBuilder();

    private final List<ToolCall> toolCalls =
            new ArrayList<>();

    private TokenUsage tokenUsage =
            TokenUsage.empty();

    private Money cost;

    public void append(String delta) {

        if (delta != null) {
            content.append(delta);
        }
    }

    public void addToolCall(ToolCall toolCall) {

        toolCalls.add(toolCall);
    }

    public void tokenUsage(TokenUsage usage) {

        this.tokenUsage = usage;
    }

    public void cost(Money cost) {

        this.cost = cost;
    }

    public ChatResult build() {

        return new ChatResult(
                content.toString(),
                tokenUsage,
                cost,
                toolCalls
        );
    }
}
