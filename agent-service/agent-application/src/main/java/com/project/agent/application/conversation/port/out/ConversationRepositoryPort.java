package com.project.agent.application.conversation.port.out;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.UserId;

import java.util.List;
import java.util.Optional;

/** Outbound port for persisting and loading {@link Conversation} aggregates (implemented in agent-adapter). */
public interface ConversationRepositoryPort {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(ConversationId id);

    List<Conversation> findByUserId(UserId userId);
}
