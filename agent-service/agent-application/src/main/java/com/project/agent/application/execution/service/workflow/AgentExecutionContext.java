package com.project.agent.application.execution.service.workflow;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.ai.ModelName;
import com.project.agent.domain.vo.ai.ProviderName;
import lombok.Builder;

import java.util.List;

/**
 * Immutable carrier for a single agent execution: bundles the conversation, the
 * {@link com.project.agent.domain.execution.agent.AgentExecution} aggregate, the resolved
 * model/provider, the assembled prompt and the enabled tools passed through the workflow.
 */
@Builder
public record AgentExecutionContext(

        Conversation conversation,

        AgentExecution execution,

        ModelName model,

        ProviderName provider,

        List<ChatMessage> prompt,

        List<String> enabledTools

) {
}
