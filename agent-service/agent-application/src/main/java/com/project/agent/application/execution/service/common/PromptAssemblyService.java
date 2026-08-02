package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.prompt.PromptTemplatePort;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the complete prompt sent to the language model.
 *
 * <p>This service converts the conversation history into provider-neutral
 * {@link ChatMessage} instances and prepends the configured system prompt.
 *
 * <p>Prompt loading is delegated to {@link PromptTemplatePort}, allowing
 * prompt templates to be sourced from the classpath, a database, object
 * storage, or another external system without changing the application
 * service.
 */
@Service
@RequiredArgsConstructor
public class PromptAssemblyService {

    private static final String DEFAULT_TEMPLATE = "default";

    private final PromptTemplatePort promptTemplatePort;

    /**
     * Builds the complete conversation history that will be sent to the LLM.
     *
     * <p>The returned history always begins with the configured system prompt
     * followed by every conversation message in chronological order.
     */
    public List<ChatMessage> buildHistory(
            Conversation conversation
    ) {

        List<ChatMessage> history = new ArrayList<>();

        history.add(
                new ChatMessage(
                        MessageRole.SYSTEM,
                        promptTemplatePort.load(DEFAULT_TEMPLATE)
                )
        );

        for (Message message : conversation.getMessages()) {

            history.add(
                    new ChatMessage(
                            message.getRole(),
                            message.getContent().value()
                    )
            );
        }

        return history;
    }
}
