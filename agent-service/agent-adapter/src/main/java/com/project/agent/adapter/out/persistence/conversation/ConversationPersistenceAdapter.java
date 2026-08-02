package com.project.agent.adapter.out.persistence.conversation;

import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Implements {@link ConversationRepositoryPort} over Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class ConversationPersistenceAdapter implements ConversationRepositoryPort {

    private final ConversationJpaRepository repository;
    private final ConversationPersistenceMapper mapper;

    @Override
    public Conversation save(Conversation conversation) {
        return mapper.toDomain(repository.save(mapper.toJpa(conversation)));
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Conversation> findByUserId(UserId userId) {
        return repository.findByUserId(userId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
