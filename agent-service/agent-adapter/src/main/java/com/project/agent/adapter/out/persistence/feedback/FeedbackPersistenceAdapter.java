package com.project.agent.adapter.out.persistence.feedback;

import com.project.agent.application.feedback.port.out.FeedbackRepositoryPort;
import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Implements {@link FeedbackRepositoryPort} over Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class FeedbackPersistenceAdapter implements FeedbackRepositoryPort {

    private final FeedbackJpaRepository repository;
    private final FeedbackPersistenceMapper mapper;

    @Override
    public Feedback save(Feedback feedback) {
        return mapper.toDomain(repository.save(mapper.toJpa(feedback)));
    }

    @Override
    public Optional<Feedback> findById(FeedbackId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Feedback> findByConversationId(ConversationId conversationId) {
        return repository.findByConversationId(conversationId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
