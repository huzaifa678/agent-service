package com.project.agent.adapter.out.persistence.conversation;

import com.project.agent.application.conversation.port.out.ConversationProjectionPort;
import com.project.agent.application.conversation.port.out.ConversationReadModelPort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.ConversationStatus;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Read-model side of the event-sourced conversation aggregate. Serves queries from the
 * materialised {@code conversations}/{@code messages} tables ({@link ConversationReadModelPort})
 * and lets the event-sourced repository keep those tables in step by upserting the aggregate's
 * current state after each append ({@link ConversationProjectionPort}). It is never the source of
 * truth — that is the event store.
 */
@Component
@RequiredArgsConstructor
public class ConversationPersistenceAdapter implements ConversationProjectionPort, ConversationReadModelPort {

    private final ConversationJpaRepository repository;
    private final ConversationPersistenceMapper mapper;

    @Override
    public void project(Conversation conversation) {
        // DELETED is a soft delete that is excluded from normal queries (see ConversationStatus),
        // so drop the row from the read model rather than keeping a tombstone that findById /
        // findByUserId would still serve. The event stream in the event store remains the record.
        if (conversation.getStatus() == ConversationStatus.DELETED) {
            repository.deleteById(conversation.getId().value());
            return;
        }

        // Write-only: unlike the old repository.save this deliberately does NOT map the persisted
        // entity back to the domain. The event-sourced repository already holds the authoritative
        // aggregate (with its correct version) and returns that; round-tripping through the
        // read model here would drop the event-sourced version (reconstitute() resets it to 0) and
        // re-read eventually-consistent projection state. The domain conversion still happens on
        // reads, in findById below (mapper::toDomain).
        repository.save(mapper.toJpa(conversation));
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
