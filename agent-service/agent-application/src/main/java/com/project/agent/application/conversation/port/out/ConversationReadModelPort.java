package com.project.agent.application.conversation.port.out;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for reading conversations from the read-model projection. The query side
 * depends on this rather than on the event-sourced write repository, so reads are served
 * from the materialised tables (and pgvector) without replaying events.
 */
public interface ConversationReadModelPort {

    Optional<Conversation> findById(ConversationId id);

    List<Conversation> findByUserId(UserId userId);
}
