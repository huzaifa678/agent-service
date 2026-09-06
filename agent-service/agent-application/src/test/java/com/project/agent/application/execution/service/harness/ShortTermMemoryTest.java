package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.service.common.builder.ConversationBuilder;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.MessageId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShortTermMemoryTest {

    @Test
    void recentWindow_keepsWholeTranscript_whenUnderLimit() {

        Conversation conversation = conversationWith(3);
        ShortTermMemory memory = new ShortTermMemory(20, new SecretRedactor(true));

        List<ChatMessage> window = memory.recentWindow(conversation);

        assertThat(window).hasSize(3);
        assertThat(window).extracting(ChatMessage::content)
                .containsExactly("m0", "m1", "m2");
    }

    @Test
    void recentWindow_keepsOnlyTheMostRecentTurns_whenOverLimit() {

        Conversation conversation = conversationWith(10);
        ShortTermMemory memory = new ShortTermMemory(3, new SecretRedactor(true));

        List<ChatMessage> window = memory.recentWindow(conversation);

        // Only the last 3, in chronological order; the older 7 fall out of short-term memory.
        assertThat(window).extracting(ChatMessage::content)
                .containsExactly("m7", "m8", "m9");
    }

    @Test
    void recentWindow_unbounded_whenLimitNonPositive() {

        Conversation conversation = conversationWith(5);
        ShortTermMemory memory = new ShortTermMemory(0, new SecretRedactor(true));

        assertThat(memory.recentWindow(conversation)).hasSize(5);
    }

    private static Conversation conversationWith(int messageCount) {
        Conversation conversation = ConversationBuilder.aConversation().build();
        for (int i = 0; i < messageCount; i++) {
            conversation.addMessage(new Message(
                    MessageId.of(UUID.randomUUID()),
                    MessageContent.of("m" + i),
                    i % 2 == 0 ? MessageRole.USER : MessageRole.ASSISTANT,
                    TokenUsage.empty()
            ));
        }
        return conversation;
    }
}
