package com.project.agent.domain.conversation;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the Conversation aggregate behaves as a correct event-sourced aggregate. */
class ConversationEventSourcingTest {

    private static Conversation newConversation() {
        return new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Test")
        );
    }

    private static Message message(String body) {
        return new Message(
                MessageId.of(UUID.randomUUID()),
                MessageContent.of(body),
                MessageRole.USER,
                TokenUsage.of(3, 0)
        );
    }

    @Test
    void mutationsRaiseContiguousSequencedEvents() {
        Conversation conversation = newConversation();
        conversation.rename(ConversationTitle.of("Renamed"));
        conversation.addMessage(message("hi"));

        List<ConversationEvent> events = conversation.pullPendingEvents();

        assertThat(events).hasSize(3);
        assertThat(events).extracting(ConversationEvent::sequence).containsExactly(1L, 2L, 3L);
        assertThat(events.get(0)).isInstanceOf(ConversationEvent.Started.class);
        assertThat(events.get(1)).isInstanceOf(ConversationEvent.Renamed.class);
        assertThat(events.get(2)).isInstanceOf(ConversationEvent.MessageAdded.class);
        assertThat(conversation.getVersion()).isEqualTo(3L);
    }

    @Test
    void pullPendingEventsDrains() {
        Conversation conversation = newConversation();
        assertThat(conversation.pullPendingEvents()).hasSize(1);
        // Second pull returns nothing: the events were drained, not merely read.
        assertThat(conversation.pullPendingEvents()).isEmpty();
    }

    @Test
    void replayReproducesState() {
        Conversation original = newConversation();
        original.rename(ConversationTitle.of("Renamed"));
        original.addMessage(message("first"));
        original.addMessage(message("second"));
        original.archive();

        List<ConversationEvent> stream = original.pullPendingEvents();
        Conversation rebuilt = Conversation.replay(stream);

        assertThat(rebuilt.getId()).isEqualTo(original.getId());
        assertThat(rebuilt.getTenantId()).isEqualTo(original.getTenantId());
        assertThat(rebuilt.getUserId()).isEqualTo(original.getUserId());
        assertThat(rebuilt.getTitle()).isEqualTo(original.getTitle());
        assertThat(rebuilt.getStatus()).isEqualTo(ConversationStatus.ARCHIVED);
        assertThat(rebuilt.getVersion()).isEqualTo(original.getVersion());
        assertThat(rebuilt.getMessages()).hasSize(2);
        assertThat(rebuilt.getMessages().get(0).getContent().value()).isEqualTo("first");
        // A rebuilt aggregate has no pending events — replay is not a mutation.
        assertThat(rebuilt.pullPendingEvents()).isEmpty();
    }

    @Test
    void snapshotPlusTailReplayEqualsFullReplay() {
        Conversation original = newConversation();
        original.rename(ConversationTitle.of("Renamed"));
        original.addMessage(message("first"));
        List<ConversationEvent> stream = original.pullPendingEvents(); // seq 1..3

        // Simulate loading a snapshot taken at version 2, then replaying only the tail (seq 3).
        Conversation atV2 = Conversation.replay(stream.subList(0, 2));
        assertThat(atV2.getVersion()).isEqualTo(2L);

        atV2.replayAll(stream.subList(2, 3));

        assertThat(atV2.getVersion()).isEqualTo(3L);
        assertThat(atV2.getMessages()).hasSize(1);
        assertThat(atV2.getTitle().value()).isEqualTo("Renamed");
    }

    @Test
    void continuingAfterReplayNumbersFromCurrentVersion() {
        Conversation original = newConversation();
        original.addMessage(message("first"));
        Conversation rebuilt = Conversation.replay(original.pullPendingEvents()); // version 2

        rebuilt.addMessage(message("second"));
        List<ConversationEvent> next = rebuilt.pullPendingEvents();

        assertThat(next).hasSize(1);
        assertThat(next.get(0).sequence()).isEqualTo(3L);
    }
}
