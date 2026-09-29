package com.project.agent.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent.application.conversation.port.out.ConversationSnapshotStorePort;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JPA-backed {@link ConversationSnapshotStorePort}. Aggregate state is serialised to a
 * self-contained JSON document ({@link SnapshotState}) rather than mirroring the domain type, so
 * refactors to {@link Conversation} do not silently invalidate stored snapshots.
 */
@Component
@RequiredArgsConstructor
public class ConversationSnapshotStoreAdapter implements ConversationSnapshotStorePort {

    private final ConversationSnapshotJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<Conversation> load(ConversationId aggregateId) {
        return repository.findById(aggregateId.value()).map(this::toDomain);
    }

    @Override
    public void save(Conversation conversation) {
        SnapshotState state = toState(conversation);
        ConversationSnapshotEntity entity = ConversationSnapshotEntity.builder()
                .aggregateId(conversation.getId().value())
                .version(conversation.getVersion())
                .state(write(state))
                .takenAt(Instant.now())
                .build();
        repository.save(entity);
    }

    private Conversation toDomain(ConversationSnapshotEntity entity) {
        SnapshotState state = read(entity.getState());
        List<Message> messages = new ArrayList<>(state.messages().size());
        for (MessageState m : state.messages()) {
            messages.add(Message.reconstitute(
                    MessageId.of(m.id()),
                    MessageContent.of(m.content()),
                    MessageRole.valueOf(m.role()),
                    TokenUsage.of(m.promptTokens(), m.completionTokens()),
                    m.createdAt()
            ));
        }
        return Conversation.fromSnapshot(
                ConversationId.of(state.id()),
                TenantId.of(state.tenantId()),
                UserId.of(state.userId()),
                state.title() == null ? null : ConversationTitle.of(state.title()),
                ConversationStatus.valueOf(state.status()),
                messages,
                state.createdAt(),
                state.updatedAt(),
                entity.getVersion()
        );
    }

    private SnapshotState toState(Conversation conversation) {
        List<MessageState> messages = conversation.getMessages().stream()
                .map(m -> new MessageState(
                        m.getId().value(),
                        m.getContent().value(),
                        m.getRole().name(),
                        m.getTokenUsage().promptTokens(),
                        m.getTokenUsage().completionTokens(),
                        m.getCreatedAt()))
                .toList();
        return new SnapshotState(
                conversation.getId().value(),
                conversation.getTenantId().value(),
                conversation.getUserId().value(),
                conversation.getTitle() == null ? null : conversation.getTitle().value(),
                conversation.getStatus().name(),
                messages,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private String write(SnapshotState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize conversation snapshot " + state.id(), e);
        }
    }

    private SnapshotState read(String json) {
        try {
            return objectMapper.readValue(json, SnapshotState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize conversation snapshot", e);
        }
    }

    /** Self-contained snapshot document. Kept independent of the domain type on purpose. */
    record SnapshotState(
            java.util.UUID id,
            java.util.UUID tenantId,
            java.util.UUID userId,
            String title,
            String status,
            List<MessageState> messages,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    record MessageState(
            java.util.UUID id,
            String content,
            String role,
            int promptTokens,
            int completionTokens,
            Instant createdAt
    ) {
    }
}
