package com.project.agent.adapter.out.persistence.eventstore;

import com.project.agent.application.conversation.port.out.ConversationEventStorePort;
import com.project.agent.application.conversation.port.out.ConversationProjectionPort;
import com.project.agent.application.conversation.port.out.ConversationReadModelPort;
import com.project.agent.application.conversation.port.out.ConversationSnapshotStorePort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.event.ConversationEvent;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventSourcedConversationRepositoryTest {

    @Mock
    private ConversationEventStorePort eventStore;
    @Mock
    private ConversationSnapshotStorePort snapshotStore;
    @Mock
    private ConversationProjectionPort projection;
    @Mock
    private ConversationReadModelPort readModel;

    /** Snapshot every 2 events, so boundaries are easy to hit in tests. */
    private EventSourcedConversationRepository repository() {
        return new EventSourcedConversationRepository(eventStore, snapshotStore, projection, readModel, 2);
    }

    private static Conversation newConversation() {
        return new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Test")
        );
    }

    @Test
    void save_appendsAtExpectedVersionAndProjects() {
        EventSourcedConversationRepository repository = repository();
        Conversation conversation = newConversation(); // 1 pending event (Started, v1)

        repository.save(conversation);

        ArgumentCaptor<Long> expected = ArgumentCaptor.forClass(Long.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ConversationEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(eventStore).append(eq(conversation.getId()), expected.capture(), events.capture());
        assertThat(expected.getValue()).isEqualTo(0L);
        assertThat(events.getValue()).hasSize(1);
        verify(projection).project(conversation);
        // Below the snapshot interval (2): no snapshot yet.
        verify(snapshotStore, never()).save(any());
        // Events were drained by save; nothing pending remains.
        assertThat(conversation.pullPendingEvents()).isEmpty();
    }

    @Test
    void save_takesSnapshotWhenCrossingInterval() {
        EventSourcedConversationRepository repository = repository();
        Conversation conversation = newConversation();     // v1 (Started)
        conversation.rename(ConversationTitle.of("Two"));   // v2 -> crosses interval 2

        repository.save(conversation);

        verify(eventStore).append(eq(conversation.getId()), eq(0L), any());
        verify(snapshotStore).save(conversation);
    }

    @Test
    void save_withNoPendingEvents_isNoOp() {
        EventSourcedConversationRepository repository = repository();
        Conversation conversation = newConversation();
        conversation.pullPendingEvents(); // drain so nothing is pending

        repository.save(conversation);

        verify(eventStore, never()).append(any(), org.mockito.ArgumentMatchers.anyLong(), any());
        verify(projection, never()).project(any());
    }

    @Test
    void findById_rebuildsFromSnapshotPlusTail() {
        EventSourcedConversationRepository repository = repository();

        // A source conversation advanced to version 3.
        Conversation source = newConversation();               // v1
        source.rename(ConversationTitle.of("Renamed"));         // v2
        source.delete();                                        // v3
        List<ConversationEvent> stream = source.pullPendingEvents();
        ConversationId id = source.getId();

        Conversation snapshotAtV2 = Conversation.replay(stream.subList(0, 2)); // v2
        when(snapshotStore.load(id)).thenReturn(Optional.of(snapshotAtV2));
        when(eventStore.loadAfter(id, 2L)).thenReturn(stream.subList(2, 3));   // tail: v3 (Deleted)

        Conversation loaded = repository.findById(id).orElseThrow();

        assertThat(loaded.getVersion()).isEqualTo(3L);
        assertThat(loaded.getStatus().name()).isEqualTo("DELETED");
    }

    @Test
    void findById_withoutSnapshot_replaysFullStream() {
        EventSourcedConversationRepository repository = repository();
        Conversation source = newConversation();
        source.rename(ConversationTitle.of("Renamed"));
        List<ConversationEvent> stream = source.pullPendingEvents();
        ConversationId id = source.getId();

        when(snapshotStore.load(id)).thenReturn(Optional.empty());
        when(eventStore.loadAfter(id, 0L)).thenReturn(stream);

        Conversation loaded = repository.findById(id).orElseThrow();

        assertThat(loaded.getVersion()).isEqualTo(2L);
        assertThat(loaded.getTitle().value()).isEqualTo("Renamed");
    }

    @Test
    void findById_unknownAggregate_returnsEmpty() {
        EventSourcedConversationRepository repository = repository();
        ConversationId id = ConversationId.of(UUID.randomUUID());
        when(snapshotStore.load(id)).thenReturn(Optional.empty());
        when(eventStore.loadAfter(id, 0L)).thenReturn(List.of());

        assertThat(repository.findById(id)).isEmpty();
    }
}
