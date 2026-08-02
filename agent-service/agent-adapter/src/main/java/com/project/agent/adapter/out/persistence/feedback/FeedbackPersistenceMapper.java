package com.project.agent.adapter.out.persistence.feedback;

import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.billing.Rating;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import com.project.agent.domain.vo.identity.MessageId;
import org.springframework.stereotype.Component;

/** Converts between the {@code Feedback} aggregate and its JPA persistence model. */
@Component
public class FeedbackPersistenceMapper {

    public FeedbackJpaEntity toJpa(Feedback feedback) {
        return FeedbackJpaEntity.builder()
                .id(feedback.getId().value())
                .conversationId(feedback.getConversationId().value())
                .messageId(feedback.getMessageId().value())
                .rating(feedback.getRating().value())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    public Feedback toDomain(FeedbackJpaEntity entity) {
        return Feedback.reconstitute(
                FeedbackId.of(entity.getId()),
                ConversationId.of(entity.getConversationId()),
                MessageId.of(entity.getMessageId()),
                Rating.of(entity.getRating()),
                entity.getComment(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
