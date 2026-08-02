package com.project.agent.adapter.out.llm.prompt;

import com.project.agent.application.execution.port.out.prompt.PromptTemplatePort;
import com.project.agent.domain.execution.agent.exception.InvalidPromptTemplateException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads prompt templates from {@code src/main/resources/prompts}.
 */
@Component
public class ClasspathPromptTemplateAdapter implements PromptTemplatePort {

    private static final String PROMPT_DIRECTORY = "prompts/";
    private static final String EXTENSION = ".txt";

    @Override
    public String load(String templateName) {

        ClassPathResource resource = new ClassPathResource(
                PROMPT_DIRECTORY + templateName + EXTENSION
        );

        if (!resource.exists()) {
            throw new InvalidPromptTemplateException(templateName);
        }

        try {
            return new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new InvalidPromptTemplateException(
                    "Failed to load prompt template: " + templateName
            );
        }
    }
}
