package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.service.common.PromptAssemblyService;
import com.project.agent.application.execution.service.common.RagRetrievalResult;
import com.project.agent.application.execution.service.common.RagService;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.vo.identity.ConversationId;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The agent's memory, as one thing. This is the harness's memory facade: the workflow asks it
 * to {@link #recall} the working context for a turn and to {@link #remember} the answer, and it
 * composes the two tiers behind that:
 *
 * <ul>
 *   <li><b>Short-term memory</b> ({@link ShortTermMemory}) — the recent turns of this
 *       conversation, verbatim and bounded to a window. Recency-based.</li>
 *   <li><b>Long-term memory</b> ({@link RagService} over the pgvector store) — everything the
 *       agent has ever said in this conversation, recalled by semantic relevance rather than
 *       recency, score-gated, and governed by the memory policy ({@link AgentMemoryHarness}).</li>
 * </ul>
 *
 * <p>A turn's prompt is assembled tier by tier: system prompt, then the (secret-redacted)
 * short-term window, then the long-term passages that cleared both the relevance bar and the
 * policy. Two protections wrap the long-term tier here:
 * <ul>
 *   <li><b>Rate limiting</b> ({@link MemoryRateLimiter}) — a per-conversation token-bucket
 *       ceiling on how often the expensive long-term operations run.</li>
 *   <li><b>Best-effort degradation</b> (see {@link RagService}) — a store failure, an open
 *       circuit, or an exhausted rate-limit budget degrades recall to the short-term window and
 *       a write to a no-op, rather than failing the turn.</li>
 * </ul>
 *
 * <p>Governance differs by tier: the long-term boundary <em>drops</em> disallowed content
 * (the memory policy, {@link AgentMemoryHarness}), while the live short-term transcript is
 * <em>redacted</em> ({@link SecretRedactor}) so the model still sees the turn in front of it.
 */
@Component
@RequiredArgsConstructor
public class AgentMemory {

    private static final Logger log = LoggerFactory.getLogger(AgentMemory.class);

    private final PromptAssemblyService promptAssemblyService;

    private final ShortTermMemory shortTermMemory;

    private final RagService longTermMemory;

    private final MemoryRateLimiter rateLimiter;

    /**
     * Assemble the working context for a turn from both memory tiers.
     *
     * @param conversation the conversation, carrying the short-term transcript
     * @param query        the user's message, used to query long-term memory
     */
    public MemoryRecall recall(Conversation conversation, String query) {

        List<ChatMessage> context = new ArrayList<>();

        // Tier 1 — system prompt.
        context.add(promptAssemblyService.systemMessage());

        // Tier 2 — short-term memory: recent turns, verbatim but secret-redacted.
        context.addAll(shortTermMemory.recentWindow(conversation));

        // Tier 3 — long-term memory: rate-limited, policy-gated, score-gated semantic recall.
        ConversationId conversationId = conversation.getId();
        double confidence = 0.0;

        if (rateLimiter.tryAcquire(conversationId)) {
            RagRetrievalResult longTerm =
                    longTermMemory.retrieveContext(conversationId, query);
            context.addAll(longTerm.contextMessages());
            confidence = longTerm.confidence();
        } else {
            log.warn(
                    "Memory rate limit reached for conversation {}; skipping long-term recall "
                            + "this turn (answering from short-term memory only).",
                    conversationId
            );
        }

        return new MemoryRecall(context, confidence);
    }

    /**
     * Commit an assistant message to long-term memory. The short-term tier needs no write —
     * it reads straight off the conversation aggregate.
     */
    public void remember(ConversationId conversationId, Message assistantMessage) {
        if (!rateLimiter.tryAcquire(conversationId)) {
            log.warn(
                    "Memory rate limit reached for conversation {}; skipping long-term persist.",
                    conversationId
            );
            return;
        }
        longTermMemory.indexAssistantResponse(conversationId, assistantMessage);
    }
}
