package com.project.agent.adapter.in.rest.mapper;

import com.project.agent.adapter.in.rest.dto.AgentExecutionResponse;
import com.project.agent.adapter.in.rest.dto.ConversationResponse;
import com.project.agent.adapter.in.rest.dto.FeedbackResponse;
import com.project.agent.adapter.in.rest.dto.MessageResponse;
import com.project.agent.adapter.in.rest.dto.ToolExecutionResponse;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.execution.tool.ToolExecution;
import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.message.Message;
import org.springframework.stereotype.Component;

/** Maps domain aggregates to REST response DTOs. */
@Component
public class RestMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId().value(),
                conversation.getTenantId().value(),
                conversation.getUserId().value(),
                conversation.getTitle() == null ? null : conversation.getTitle().value(),
                conversation.getStatus().name(),
                conversation.messageCount(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId().value(),
                message.getRole().name(),
                message.getContent().value(),
                message.getTokenUsage().promptTokens(),
                message.getTokenUsage().completionTokens(),
                message.getTokenUsage().totalTokens(),
                message.getCreatedAt()
        );
    }

    public AgentExecutionResponse toResponse(AgentExecution execution) {
        return new AgentExecutionResponse(
                execution.getId().value(),
                execution.getConversationId().value(),
                execution.getModelName().value(),
                execution.getProviderName().value(),
                execution.getStatus().name(),
                execution.getTokenUsage().promptTokens(),
                execution.getTokenUsage().completionTokens(),
                execution.getTokenUsage().totalTokens(),
                execution.getCost().amount(),
                execution.getCost().currency().getCurrencyCode(),
                execution.getLatency().toMillis(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getToolExecutions().stream().map(this::toResponse).toList()
        );
    }

    public ToolExecutionResponse toResponse(ToolExecution tool) {
        return new ToolExecutionResponse(
                tool.getId().value(),
                tool.getToolName().value(),
                tool.getRequest().value(),
                tool.getResponse() == null ? null : tool.getResponse().value(),
                tool.getStatus().name(),
                tool.getLatency().toMillis(),
                tool.getStartedAt(),
                tool.getCompletedAt()
        );
    }

    public FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId().value(),
                feedback.getConversationId().value(),
                feedback.getMessageId().value(),
                feedback.getRating().value(),
                feedback.isPositive(),
                feedback.getComment(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
