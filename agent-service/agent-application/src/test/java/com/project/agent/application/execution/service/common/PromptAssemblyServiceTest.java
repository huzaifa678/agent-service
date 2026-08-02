package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.prompt.PromptTemplatePort;
import com.project.agent.application.execution.service.common.builder.ConversationBuilder;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.MessageId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptAssemblyServiceTest {

    @Mock
    private PromptTemplatePort promptTemplatePort;

    @InjectMocks
    private PromptAssemblyService service;

    @Test
    void buildHistory_addsSystemPromptFirst() {

        Conversation conversation = ConversationBuilder.aConversation()
                .build();

        when(promptTemplatePort.load("default"))
                .thenReturn("You are a helpful assistant.");

        List<ChatMessage> history = service.buildHistory(conversation);

        assertEquals(1, history.size());
        assertEquals(MessageRole.SYSTEM, history.get(0).role());
        assertEquals(
                "You are a helpful assistant.",
                history.get(0).content()
        );

        verify(promptTemplatePort).load("default");
    }

    @Test
    void buildHistory_includesConversationMessagesInOrder() {

        Conversation conversation = ConversationBuilder.aConversation()
                .build();

        conversation.addMessage(
                new Message(
                        MessageId.of(UUID.randomUUID()),
                        MessageContent.of("Hello"),
                        MessageRole.USER,
                        TokenUsage.empty()
                )
        );

        conversation.addMessage(
                new Message(
                        MessageId.of(UUID.randomUUID()),
                        MessageContent.of("Hi!"),
                        MessageRole.ASSISTANT,
                        TokenUsage.empty()
                )
        );

        when(promptTemplatePort.load("default"))
                .thenReturn("System Prompt");

        List<ChatMessage> history = service.buildHistory(conversation);

        assertEquals(3, history.size());

        assertEquals(MessageRole.SYSTEM, history.get(0).role());
        assertEquals("System Prompt", history.get(0).content());

        assertEquals(MessageRole.USER, history.get(1).role());
        assertEquals("Hello", history.get(1).content());

        assertEquals(MessageRole.ASSISTANT, history.get(2).role());
        assertEquals("Hi!", history.get(2).content());

        verify(promptTemplatePort).load("default");
    }

    @Test
    void buildHistory_emptyConversation_returnsOnlySystemPrompt() {

        Conversation conversation = ConversationBuilder.aConversation()
                .build();

        when(promptTemplatePort.load("default"))
                .thenReturn("System Prompt");

        List<ChatMessage> history = service.buildHistory(conversation);

        assertEquals(1, history.size());
        assertEquals(MessageRole.SYSTEM, history.get(0).role());
        assertEquals("System Prompt", history.get(0).content());

        verify(promptTemplatePort).load("default");
    }
}
