package com.project.agent.application.execution.port.out.llm.model;

import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;

import java.util.List;

/**
 * Result of one LLM invocation: the assistant text, the provider-reported token
 * usage and computed cost, and any tool calls the model requested.
 */
public record ChatResult(
        String content,
        TokenUsage tokenUsage,
        Money cost,
        List<ToolCall> toolCalls
) {}
