package com.project.agent.application.execution.service.common.builder;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;

import java.util.List;

public class AgentExecutionContextBuilder {

    private Conversation conversation =
            ConversationBuilder.aConversation().build();

    private AgentExecution execution =
            AgentExecutionBuilder.anExecution().build();

    private ModelName model =
            ModelName.of("gpt-4o");

    private ProviderName provider =
            ProviderName.of("openai");

    private List<ChatMessage> prompt =
            List.of(
                    new ChatMessage(
                            MessageRole.USER,
                            "Hello"
                    )
            );

    private List<String> enabledTools =
            List.of();

    public static AgentExecutionContextBuilder aContext() {
        return new AgentExecutionContextBuilder();
    }

    public AgentExecutionContextBuilder withConversation(
            Conversation conversation
    ) {
        this.conversation = conversation;
        return this;
    }

    public AgentExecutionContextBuilder withExecution(
            AgentExecution execution
    ) {
        this.execution = execution;
        return this;
    }

    public AgentExecutionContextBuilder withModel(
            ModelName model
    ) {
        this.model = model;
        return this;
    }

    public AgentExecutionContextBuilder withProvider(
            ProviderName provider
    ) {
        this.provider = provider;
        return this;
    }

    public AgentExecutionContextBuilder withPrompt(
            List<ChatMessage> prompt
    ) {
        this.prompt = prompt;
        return this;
    }

    public AgentExecutionContextBuilder withEnabledTools(
            List<String> enabledTools
    ) {
        this.enabledTools = enabledTools;
        return this;
    }

    public AgentExecutionContext build() {
        return AgentExecutionContext.builder()
                .conversation(conversation)
                .execution(execution)
                .model(model)
                .provider(provider)
                .prompt(prompt)
                .enabledTools(enabledTools)
                .build();
    }
}