package com.project.agent.application.execution.service.workflow;

import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.service.common.*;
import com.project.agent.application.execution.service.harness.AgentBudgetGuard;
import com.project.agent.application.execution.service.harness.AgentGuardrailHarness;
import com.project.agent.application.execution.service.harness.AgentMemory;
import com.project.agent.application.execution.service.harness.MemoryRecall;
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
 * prompt, assembles the working context from {@link AgentMemory} (short-term + long-term
 * tiers), runs tools, and persists/records the execution and its domain events. On completion
 * the answer is committed back to memory. {@link #prepare} builds the
 * {@link AgentExecutionContext} before the LLM call; {@link #finish} records the result
 * afterwards.
 */
@Service
@RequiredArgsConstructor
public class AgentExecutionWorkflow {

    private final ConversationRepositoryPort conversationRepository;

    private final AgentExecutionRepositoryPort agentExecutionRepository;

    private final ConversationValidationService conversationValidationService;

    private final PromptValidationService promptValidationService;

    private final ToolExecutionService toolExecutionService;

    private final AgentMemory agentMemory;

    private final AgentGuardrailHarness guardrailHarness;

    private final AgentBudgetGuard budgetGuard;

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

        // Cost governance: refuse the turn before any LLM spend if this conversation has
        // already exhausted its token/cost budget.
        budgetGuard.check(conversationId);

        // Input guardrail: screen the user's message for prompt-injection / jailbreak before
        // it is added to the conversation, assembled into the prompt, or captured into memory.
        guardrailHarness.guardInput(conversationId, command.userMessage());

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

        // Assemble the working context from both memory tiers (short-term recent turns +
        // policy-gated long-term recall). The user message added above is now part of the
        // short-term window.
        MemoryRecall recall =
                agentMemory.recall(
                        conversation,
                        command.userMessage()
                );

        AgentExecution execution =
                AgentExecution.start(
                        AgentExecutionId.of(UUID.randomUUID()),
                        conversationId,
                        model,
                        provider
                );

        execution.recordRetrievalConfidence(recall.retrievalConfidence());

        return AgentExecutionContext.builder()
                .conversation(conversation)
                .execution(execution)
                .model(model)
                .provider(provider)
                .prompt(recall.context())
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

        ConversationId conversationId =
                context.execution().getConversationId();

        // Output guardrail: screen the answer (leakage, toxicity, minimum grounding) before any
        // tool side-effects are recorded, the answer is returned, or it is remembered. A block
        // aborts finalisation — nothing is persisted.
        Double grounding = context.execution().getRetrievalConfidence();
        guardrailHarness.guardOutput(
                conversationId,
                result.content(),
                grounding == null ? 0.0 : grounding
        );

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

        agentMemory.remember(
                conversationId,
                assistantMessage
        );

        // Cost governance: accumulate this turn's token usage and cost against the
        // conversation's budget so a later turn can be refused once a ceiling is reached.
        budgetGuard.record(
                conversationId,
                result.tokenUsage(),
                result.cost()
        );

        return savedExecution;
    }
}
