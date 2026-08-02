package com.project.agent.application.execution.service.workflow;

import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.service.common.*;
import com.project.agent.application.shared.port.out.DomainEventPublisherPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.ConversationNotFoundException;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the non-LLM steps of an agent execution: validates the conversation and
 * prompt, assembles the prompt (with RAG context), runs tools, and persists/records the
 * execution and its domain events. {@link #prepare} builds the {@link AgentExecutionContext}
 * before the LLM call; {@link #finish} records the result afterwards.
 */
@Service
@RequiredArgsConstructor
public class AgentExecutionWorkflow {

    private final ConversationRepositoryPort conversationRepository;

    private final AgentExecutionRepositoryPort agentExecutionRepository;

    private final DomainEventPublisherPort eventPublisher;

    private final ConversationValidationService conversationValidationService;

    private final PromptValidationService promptValidationService;

    private final PromptAssemblyService promptAssemblyService;

    private final ToolExecutionService toolExecutionService;

    private final RagService ragService;

    /**
     * Performs all preparation required before invoking the LLM.
     */
    public AgentExecutionContext prepare(
            RunAgentCommand command
    ) {

        ConversationId conversationId =
                ConversationId.of(command.conversationId());

        Conversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(() ->
                                ConversationNotFoundException.of(
                                        conversationId.toString()
                                )
                        );

        conversationValidationService.validate(conversation);

        ModelName model =
                ModelName.of(command.modelName());

        ProviderName provider =
                ProviderName.of(command.providerName());

        promptValidationService.validate(
                conversation,
                model,
                command.userMessage()
        );

        List<String> enabledTools =
                toolExecutionService.validateTools(
                        command.enabledTools()
                );

        Message userMessage = new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of(command.userMessage()),
                MessageRole.USER,
                TokenUsage.empty()
        );

        conversation.addMessage(userMessage);

        List<ChatMessage> prompt =
                promptAssemblyService.buildHistory(
                        conversation
                );

        prompt.addAll(
                ragService.retrieveContext(
                        conversationId,
                        command.userMessage()
                )
        );

        AgentExecution execution =
                AgentExecution.start(
                        AgentExecutionId.of(UUID.randomUUID()),
                        conversationId,
                        model,
                        provider
                );

        return AgentExecutionContext.builder()
                .conversation(conversation)
                .execution(execution)
                .model(model)
                .provider(provider)
                .prompt(prompt)
                .enabledTools(enabledTools)
                .build();
    }

    /**
     * Completes an execution after the LLM finishes successfully.
     */
    public AgentExecution finish(
            AgentExecutionContext context,
            ChatResult result
    ) {

        toolExecutionService.execute(
                context.execution(),
                result.toolCalls()
        );

        Message assistantMessage = new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of(result.content()),
                MessageRole.ASSISTANT,
                result.tokenUsage()
        );

        context.conversation().addMessage(
                assistantMessage
        );

        context.execution().complete(
                result.tokenUsage(),
                result.cost()
        );

        AgentExecution savedExecution =
                agentExecutionRepository.save(
                        context.execution()
                );

        conversationRepository.save(
                context.conversation()
        );

        ragService.indexAssistantResponse(
                context.execution().getConversationId(),
                assistantMessage
        );

        eventPublisher.publishAll(
                context.conversation().domainEvents()
        );

        context.conversation().clearDomainEvents();

        return savedExecution;
    }
}
