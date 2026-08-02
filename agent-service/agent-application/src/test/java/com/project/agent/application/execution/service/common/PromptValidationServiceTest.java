package com.project.agent.application.execution.service.common;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.execution.agent.exception.InvalidPromptTemplateException;
import com.project.agent.domain.execution.agent.exception.PromptTooLargeException;
import com.project.agent.domain.execution.agent.exception.UnsupportedModelException;
import com.project.agent.domain.vo.ai.ModelName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromptValidationServiceTest {

    private PromptValidationService service;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        service = new PromptValidationService();
    }

    @Test
    void validate_withValidPromptAndModel_doesNotThrow() {

        assertDoesNotThrow(() ->
                service.validate(
                        conversation,
                        ModelName.of("gpt-4o"),
                        "Hello!"
                )
        );
    }

    @Test
    void validate_withUnsupportedModel_throws() {

        assertThrows(
                UnsupportedModelException.class,
                () -> service.validate(
                        conversation,
                        ModelName.of("llama-2"),
                        "Hello"
                )
        );
    }

    @Test
    void validate_withNullPrompt_throws() {

        assertThrows(
                InvalidPromptTemplateException.class,
                () -> service.validate(
                        conversation,
                        ModelName.of("gpt-4o"),
                        null
                )
        );
    }

    @Test
    void validate_withBlankPrompt_throws() {

        assertThrows(
                InvalidPromptTemplateException.class,
                () -> service.validate(
                        conversation,
                        ModelName.of("gpt-4o"),
                        "   "
                )
        );
    }

    @Test
    void validate_withPromptExceedingMaxLength_throws() {

        String prompt = "a".repeat(100_001);

        assertThrows(
                PromptTooLargeException.class,
                () -> service.validate(
                        conversation,
                        ModelName.of("gpt-4o"),
                        prompt
                )
        );
    }

    @Test
    void validateModel_withSupportedModel_doesNotThrow() {

        assertDoesNotThrow(() ->
                service.validateModel(
                        ModelName.of("gpt-4o-mini")
                )
        );
    }

    @Test
    void validateModel_withUnsupportedModel_throws() {

        assertThrows(
                UnsupportedModelException.class,
                () -> service.validateModel(
                        ModelName.of("unknown-model")
                )
        );
    }

    @Test
    void validatePrompt_withValidPrompt_doesNotThrow() {

        assertDoesNotThrow(() ->
                service.validatePrompt(
                        "Hello world"
                )
        );
    }

    @Test
    void validatePrompt_withNullPrompt_throws() {

        assertThrows(
                InvalidPromptTemplateException.class,
                () -> service.validatePrompt(
                        null
                )
        );
    }

    @Test
    void validatePrompt_withBlankPrompt_throws() {

        assertThrows(
                InvalidPromptTemplateException.class,
                () -> service.validatePrompt(
                        ""
                )
        );
    }

    @Test
    void validatePrompt_withTooLargePrompt_throws() {

        String prompt = "a".repeat(100_001);

        assertThrows(
                PromptTooLargeException.class,
                () -> service.validatePrompt(
                        prompt
                )
        );
    }
}
