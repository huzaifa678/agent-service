package com.project.agent.application.execution.service.common.builder;

import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;

import java.util.UUID;

/**
 * Test builder for {@link AgentExecution}.
 */
public class AgentExecutionBuilder {

    private UUID executionId = UUID.randomUUID();
    private UUID conversationId = UUID.randomUUID();
    private String model = "gpt-4o";
    private String provider = "openai";

    public static AgentExecutionBuilder anExecution() {
        return new AgentExecutionBuilder();
    }

    public AgentExecutionBuilder withConversationId(UUID id) {
        this.conversationId = id;
        return this;
    }

    public AgentExecutionBuilder withModel(String model) {
        this.model = model;
        return this;
    }

    public AgentExecutionBuilder withProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public AgentExecution build() {

        return AgentExecution.start(
                AgentExecutionId.of(executionId),
                ConversationId.of(conversationId),
                ModelName.of(model),
                ProviderName.of(provider)
        );
    }
}
