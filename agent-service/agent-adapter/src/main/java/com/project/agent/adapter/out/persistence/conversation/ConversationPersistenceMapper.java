package com.project.agent.adapter.out.persistence.conversation;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.ConversationStatus;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Converts between the {@code Conversation} aggregate and its JPA persistence model. */
@Component
public class ConversationPersistenceMapper {

    public ConversationJpaEntity toJpa(Conversation conversation) {
        List<MessageJpaEntity> messages = conversation.getMessages().stream()
                .map(this::toJpa)
                .collect(Collectors.toCollection(ArrayList::new));

        return ConversationJpaEntity.builder()
                .id(conversation.getId().value())
                .tenantId(conversation.getTenantId().value())
                .userId(conversation.getUserId().value())
                .title(conversation.getTitle() == null ? null : conversation.getTitle().value())
                .status(conversation.getStatus().name())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .messages(messages)
                .build();
    }

    public Conversation toDomain(ConversationJpaEntity entity) {
        List<Message> messages = entity.getMessages().stream()
                .map(this::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));

        return Conversation.reconstitute(
                ConversationId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                UserId.of(entity.getUserId()),
                entity.getTitle() == null ? null : ConversationTitle.of(entity.getTitle()),
                ConversationStatus.valueOf(entity.getStatus()),
                messages,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private MessageJpaEntity toJpa(Message message) {
        return MessageJpaEntity.builder()
                .id(message.getId().value())
                .content(message.getContent().value())
                .role(message.getRole().name())
                .promptTokens(message.getTokenUsage().promptTokens())
                .completionTokens(message.getTokenUsage().completionTokens())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private Message toDomain(MessageJpaEntity entity) {
        return Message.reconstitute(
                MessageId.of(entity.getId()),
                MessageContent.of(entity.getContent()),
                MessageRole.valueOf(entity.getRole()),
                TokenUsage.of(entity.getPromptTokens(), entity.getCompletionTokens()),
                entity.getCreatedAt()
        );
    }
}
