package com.project.agent.adapter.out.persistence.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent.domain.conversation.event.ConversationEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Serialises {@link ConversationEvent}s to/from the JSON stored in the event store. A stable
 * {@code eventType} tag is persisted alongside the payload so that events can be re-materialised
 * into the right record on replay. The tag is decoupled from the Java class name on purpose:
 * classes may be refactored, but a stored type string must never change.
 */
@Component
public class ConversationEventSerializer {

    private final ObjectMapper objectMapper;

    private final Map<String, Class<? extends ConversationEvent>> typeToClass = Map.of(
            "conversation.started", ConversationEvent.Started.class,
            "conversation.renamed", ConversationEvent.Renamed.class,
            "conversation.message-added", ConversationEvent.MessageAdded.class,
            "conversation.message-removed", ConversationEvent.MessageRemoved.class,
            "conversation.archived", ConversationEvent.Archived.class,
            "conversation.deleted", ConversationEvent.Deleted.class,
            "conversation.activated", ConversationEvent.Activated.class
    );

    private final Map<Class<? extends ConversationEvent>, String> classToType;

    public ConversationEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.classToType = typeToClass.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    /** Stable persisted type tag for an event. */
    public String typeOf(ConversationEvent event) {
        String type = classToType.get(event.getClass());
        if (type == null) {
            throw new IllegalArgumentException("No stored type mapping for event " + event.getClass().getName());
        }
        return type;
    }

    public String serialize(ConversationEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // Serialisation failure is a programming error (bad model), not a transient one.
            throw new IllegalStateException("Failed to serialize conversation event " + event.eventId(), e);
        }
    }

    public ConversationEvent deserialize(String eventType, String payload) {
        Class<? extends ConversationEvent> target = typeToClass.get(eventType);
        if (target == null) {
            throw new IllegalStateException("Unknown conversation event type '" + eventType + "' in event store");
        }
        try {
            return objectMapper.readValue(payload, target);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize conversation event of type " + eventType, e);
        }
    }
}
