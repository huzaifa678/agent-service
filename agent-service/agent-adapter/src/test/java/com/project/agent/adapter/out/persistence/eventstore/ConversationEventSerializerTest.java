package com.project.agent.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent.domain.conversation.event.ConversationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips every event type through the serializer with a Jackson mapper configured like the
 * application's (Java-time + record parameter names), so the stored JSON contract is verified
 * without a database.
 */
class ConversationEventSerializerTest {

    // findAndRegisterModules picks up jackson-datatype-jsr310 and the parameter-names module,
    // matching the Spring Boot ObjectMapper the serializer is given at runtime.
    private final ConversationEventSerializer serializer =
            new ConversationEventSerializer(new ObjectMapper().findAndRegisterModules());

    private <T extends ConversationEvent> void assertRoundTrips(T event) {
        String type = serializer.typeOf(event);
        String json = serializer.serialize(event);
        ConversationEvent restored = serializer.deserialize(type, json);
        // Records have value equality, so a faithful round-trip restores an equal event.
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void allEventTypesRoundTrip() {
        UUID conversationId = UUID.randomUUID();
        assertRoundTrips(ConversationEvent.Started.of(
                conversationId, 1L, UUID.randomUUID(), UUID.randomUUID(), "Title"));
        assertRoundTrips(ConversationEvent.Renamed.of(conversationId, 2L, "Renamed"));
        assertRoundTrips(ConversationEvent.MessageAdded.of(
                conversationId, 3L, UUID.randomUUID(), "hello", "USER", 3, 0, Instant.now()));
        assertRoundTrips(ConversationEvent.MessageRemoved.of(conversationId, 4L, UUID.randomUUID()));
        assertRoundTrips(ConversationEvent.Archived.of(conversationId, 5L));
        assertRoundTrips(ConversationEvent.Deleted.of(conversationId, 6L));
        assertRoundTrips(ConversationEvent.Activated.of(conversationId, 7L));
    }

    @Test
    void typeTagsAreStableAndDistinct() {
        UUID id = UUID.randomUUID();
        assertThat(serializer.typeOf(ConversationEvent.Started.of(id, 1L, id, id, "t")))
                .isEqualTo("conversation.started");
        assertThat(serializer.typeOf(ConversationEvent.MessageAdded.of(id, 2L, id, "c", "USER", 1, 0, Instant.now())))
                .isEqualTo("conversation.message-added");
    }

    @Test
    void unknownTypeTagIsRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> serializer.deserialize("conversation.unknown", "{}"))
                .isInstanceOf(IllegalStateException.class);
    }
}
