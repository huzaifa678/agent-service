package com.project.agent.application.execution.port.out.llm.model;

import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;

import java.util.List;

/** Everything the LLM adapter needs for one invocation. Carries domain value objects only. */
public record ChatRequest(
        ModelName model,
        ProviderName provider,
        List<ChatMessage> messages,
        List<String> enabledTools
) {}
