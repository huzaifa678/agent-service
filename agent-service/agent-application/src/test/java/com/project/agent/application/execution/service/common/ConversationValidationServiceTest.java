package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.service.common.builder.ConversationBuilder;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.InvalidConversationStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationValidationServiceTest {

    private ConversationValidationService service;

    @BeforeEach
    void setUp() {
        service = new ConversationValidationService();
    }

    @Test
    void validate_activeConversation_doesNotThrow() {

        Conversation conversation = ConversationBuilder.aConversation()
                .build();

        assertDoesNotThrow(() ->
                service.validate(conversation)
        );
    }

    @Test
    void validate_archivedConversation_throwsInvalidConversationStateException() {

        Conversation conversation = ConversationBuilder.aConversation()
                .archived()
                .build();

        assertThrows(
                InvalidConversationStateException.class,
                () -> service.validate(conversation)
        );
    }
}
