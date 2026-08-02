package com.project.agent.application.execution.service.common;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.execution.agent.exception.InvalidPromptTemplateException;
import com.project.agent.domain.execution.agent.exception.PromptTooLargeException;
import com.project.agent.domain.execution.agent.exception.UnsupportedModelException;
import com.project.agent.domain.vo.ai.ModelName;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Validates all constraints that can be verified before invoking the LLM.
 *
 * <p>This service owns model validation and prompt validation only.
 * Provider-specific constraints such as context-window size and token
 * limits are enforced by the LLM provider itself and translated into
 * domain exceptions by the LLM adapter.
 *
 * <p>Validation limits are currently hard-coded placeholders and should
 * eventually be supplied through typed configuration properties.
 */
@Service
public class PromptValidationService {

    // TODO Move to configuration.

    private static final Set<String> SUPPORTED_MODELS = Set.of(
            "gpt-4o",
            "gpt-4o-mini",
            "claude-3-5-sonnet-20241022"
    );

    // Prevent obviously unreasonable requests before contacting the provider.
    private static final int MAX_PROMPT_CHARS = 100_000;

    /**
     * Performs every validation required before an agent execution begins.
     */
    public void validate(
            Conversation conversation,
            ModelName model,
            String prompt
    ) {
        validateModel(model);
        validatePrompt(prompt);
    }

    /**
     * Ensures the requested model is supported.
     */
    public void validateModel(ModelName model) {

        if (!SUPPORTED_MODELS.contains(model.value())) {
            throw new UnsupportedModelException(model.value());
        }
    }

    /**
     * Ensures the user prompt is present and does not exceed the maximum
     * configured size.
     */
    public void validatePrompt(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new InvalidPromptTemplateException("user-message");
        }

        int length = prompt.length();

        if (length > MAX_PROMPT_CHARS) {
            throw new PromptTooLargeException(
                    length,
                    MAX_PROMPT_CHARS
            );
        }
    }
}
