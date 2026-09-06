package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.memory.MemoryAccessRequest;
import com.project.agent.application.execution.port.out.memory.MemoryDecision;
import com.project.agent.application.execution.port.out.memory.MemoryOperation;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyMetricsPort;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyPort;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyUnavailableException;
import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.domain.vo.identity.ConversationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The governance envelope around the agent's long-term memory.
 *
 * <p>Score gating ({@code RagRetrievalPolicy}) decides whether a passage is <em>relevant</em>.
 * The harness decides whether it is <em>allowed</em> — a separate question, and one that
 * belongs to policy rather than code. Every recall is filtered and every persist is vetoed
 * through {@link MemoryPolicyPort}, which the production adapter backs with an OPA/Rego
 * policy so the rules (secret redaction, tenant retention opt-out, cross-tenant isolation)
 * can change without a redeploy.
 *
 * <p>When the decision engine is unreachable the harness applies a configured posture rather
 * than failing the turn:
 * <ul>
 *   <li><b>fail-open</b> (default {@code false}) — treat the unreachable engine as an allow.
 *       Keeps the agent answering when governance is best-effort.</li>
 *   <li><b>fail-closed</b> — drop the recall / skip the persist. The safe default: a memory
 *       write is best-effort anyway, and an ungoverned recall is worse than a thinner prompt.</li>
 * </ul>
 */
@Component
public class AgentMemoryHarness {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryHarness.class);

    private final MemoryPolicyPort policy;
    private final MemoryPolicyMetricsPort metrics;
    private final boolean failOpen;

    public AgentMemoryHarness(
            MemoryPolicyPort policy,
            MemoryPolicyMetricsPort metrics,
            @Value("${agent.memory.policy.fail-open:false}") boolean failOpen
    ) {
        this.policy = policy;
        this.metrics = metrics;
        this.failOpen = failOpen;
    }

    /**
     * Filter relevance-gated passages down to the ones policy permits to re-enter this
     * conversation's context. Order is preserved; a denied passage is dropped, not redacted.
     */
    public List<RetrievedPassage> guardRecall(
            ConversationId conversationId,
            List<RetrievedPassage> gated
    ) {

        List<RetrievedPassage> allowed = new ArrayList<>(gated.size());

        for (RetrievedPassage passage : gated) {

            MemoryAccessRequest request = MemoryAccessRequest.recall(
                    conversationId.toString(),
                    passage.content(),
                    Map.of("score", passage.score())
            );

            if (permitted(request)) {
                allowed.add(passage);
            }
        }

        int dropped = gated.size() - allowed.size();
        if (dropped > 0) {
            log.info(
                    "Memory recall policy dropped {} of {} passage(s) for conversation {}.",
                    dropped,
                    gated.size(),
                    conversationId
            );
        }

        return allowed;
    }

    /**
     * Decide whether an assistant message may be committed to long-term memory.
     *
     * @return {@code true} if the caller should persist the content
     */
    public boolean guardPersist(
            ConversationId conversationId,
            String content
    ) {

        MemoryAccessRequest request = MemoryAccessRequest.persist(
                conversationId.toString(),
                content,
                Map.of()
        );

        boolean allowed = permitted(request);
        if (!allowed) {
            log.info(
                    "Memory persist policy blocked storing an assistant message for conversation {}.",
                    conversationId
            );
        }
        return allowed;
    }

    private boolean permitted(MemoryAccessRequest request) {

        try {

            MemoryDecision decision = policy.decide(request);
            metrics.recordDecision(request.operation(), decision.allowed());

            if (!decision.allowed()) {
                log.debug(
                        "Memory {} denied: {}",
                        request.operation(),
                        decision.reason()
                );
            }
            return decision.allowed();

        } catch (MemoryPolicyUnavailableException e) {

            metrics.recordUnavailable(request.operation());
            log.warn(
                    "Memory policy engine unavailable for {}; falling back to fail-{}.",
                    request.operation(),
                    failOpen ? "open (allow)" : "closed (deny)",
                    e
            );
            return failOpen;
        }
    }
}
