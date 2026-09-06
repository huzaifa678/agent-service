package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Short-term (working) memory: the most recent turns of the live conversation, verbatim.
 *
 * <p>This is the fast, exact tier — no embedding, no similarity search, just the tail of the
 * transcript. It is bounded by {@code agent.memory.short-term.max-messages} so a long-running
 * conversation cannot grow the prompt without limit (and blow the context window); the oldest
 * turns fall out of short-term memory and survive only in long-term memory, where they are
 * recalled by relevance instead of recency.
 *
 * <p>A non-positive limit means "unbounded" — keep the whole transcript, which is the
 * behaviour the service had before the window was introduced.
 *
 * <p>Every message in the window is passed through {@link SecretRedactor} before it enters the
 * prompt, so a credential a user pasted into the conversation is masked rather than echoed back
 * (and never captured into long-term memory on the next turn).
 */
@Component
public class ShortTermMemory {

    private final int maxMessages;
    private final SecretRedactor redactor;

    public ShortTermMemory(
            @Value("${agent.memory.short-term.max-messages:20}") int maxMessages,
            SecretRedactor redactor
    ) {
        this.maxMessages = maxMessages;
        this.redactor = redactor;
    }

    /**
     * The recent-turn window as provider-neutral {@link ChatMessage}s, in chronological order,
     * with secrets redacted.
     */
    public List<ChatMessage> recentWindow(Conversation conversation) {

        List<Message> messages = conversation.getMessages();

        List<Message> window =
                (maxMessages > 0 && messages.size() > maxMessages)
                        ? messages.subList(messages.size() - maxMessages, messages.size())
                        : messages;

        return window.stream()
                .map(message -> new ChatMessage(
                        message.getRole(),
                        redactor.redact(message.getContent().value())
                ))
                .toList();
    }
}
