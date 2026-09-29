package com.project.agent.adapter.out.persistence.eventstore;

import com.project.agent.adapter.support.PostgreSQLContainerConfig;
import com.project.agent.application.conversation.port.out.ConversationEventStorePort;
import com.project.agent.application.conversation.port.out.ConversationRepositoryPort;
import com.project.agent.application.conversation.port.out.ConversationSnapshotStorePort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.ConversationStatus;
import com.project.agent.domain.conversation.event.ConversationEvent;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the event store, snapshot store, and the event-sourced repository against a real
 * Postgres, which is the only place the {@code jsonb} mapping and event (de)serialisation are
 * actually validated end to end.
 */
@SpringJUnitConfig
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
class ConversationEventStoreAdapterIT extends PostgreSQLContainerConfig {

    @Autowired
    private ConversationEventStorePort eventStore;
    @Autowired
    private ConversationSnapshotStorePort snapshotStore;
    @Autowired
    private ConversationRepositoryPort repository; // the @Primary event-sourced impl

    private static Conversation newConversation() {
        return new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Test")
        );
    }

    @Test
    void appendThenLoad_roundTripsEventsInOrder() {
        Conversation conversation = newConversation();          // v1 Started
        conversation.rename(ConversationTitle.of("Renamed"));   // v2 Renamed
        List<ConversationEvent> events = conversation.pullPendingEvents();

        eventStore.append(conversation.getId(), 0L, events);

        List<ConversationEvent> loaded = eventStore.loadAfter(conversation.getId(), 0L);
        assertThat(loaded).extracting(ConversationEvent::sequence).containsExactly(1L, 2L);
        assertThat(loaded.get(0)).isInstanceOf(ConversationEvent.Started.class);
        assertThat(loaded.get(1)).isInstanceOf(ConversationEvent.Renamed.class);
        assertThat(((ConversationEvent.Renamed) loaded.get(1)).title()).isEqualTo("Renamed");
    }

    @Test
    void append_atWrongExpectedVersion_failsOptimisticLock() {
        Conversation conversation = newConversation();
        List<ConversationEvent> first = conversation.pullPendingEvents(); // v1
        eventStore.append(conversation.getId(), 0L, first);

        // A second writer that still thinks the stream is empty tries to write sequence 1 again.
        Conversation clash = Conversation.replay(List.of(
                ConversationEvent.Started.of(conversation.getId().value(), 1L,
                        UUID.randomUUID(), UUID.randomUUID(), "Clash")));
        assertThatThrownBy(() ->
                eventStore.append(conversation.getId(), 0L, clash.pullPendingEvents()))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void snapshot_roundTrips() {
        Conversation conversation = newConversation();
        conversation.addMessage(new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of("hi"),
                MessageRole.USER,
                TokenUsage.of(3, 0)));

        snapshotStore.save(conversation);

        Conversation loaded = snapshotStore.load(conversation.getId()).orElseThrow();
        assertThat(loaded.getVersion()).isEqualTo(conversation.getVersion());
        assertThat(loaded.getMessages()).hasSize(1);
        assertThat(loaded.getMessages().get(0).getContent().value()).isEqualTo("hi");
    }

    @Test
    void repository_saveThenFindById_rebuildsAggregate() {
        Conversation conversation = newConversation();
        conversation.addMessage(new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of("first"),
                MessageRole.USER,
                TokenUsage.of(5, 0)));
        repository.save(conversation);

        Conversation loaded = repository.findById(conversation.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(loaded.getVersion()).isEqualTo(2L);
        assertThat(loaded.getMessages()).hasSize(1);
        assertThat(loaded.getMessages().get(0).getContent().value()).isEqualTo("first");
    }
}
