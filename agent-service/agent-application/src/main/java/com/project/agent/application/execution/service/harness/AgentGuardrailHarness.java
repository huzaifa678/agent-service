package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.exception.InputGuardrailViolationException;
import com.project.agent.application.execution.exception.OutputGuardrailViolationException;
import com.project.agent.application.execution.port.out.guardrail.GuardrailDecision;
import com.project.agent.application.execution.port.out.guardrail.GuardrailDirection;
import com.project.agent.application.execution.port.out.guardrail.GuardrailMetricsPort;
import com.project.agent.application.execution.port.out.guardrail.GuardrailPolicyPort;
import com.project.agent.application.execution.port.out.guardrail.GuardrailPolicyUnavailableException;
import com.project.agent.application.execution.port.out.guardrail.GuardrailRequest;
import com.project.agent.domain.vo.identity.ConversationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The governance envelope around model I/O — the third boundary the harness guards, alongside
 * memory ({@link AgentMemoryHarness}) and tools ({@link AgentToolHarness}).
 *
 * <p>The memory harness screens what crosses the <em>memory</em> boundary; this one screens
 * what crosses the <em>model</em> boundary:
 * <ul>
 *   <li><b>input</b> — the user's message before it reaches the prompt. Blocks detected
 *       prompt-injection / jailbreak attempts, so a crafted message never steers the model or
 *       lands in long-term memory.</li>
 *   <li><b>output</b> — the assistant's answer before it is returned and remembered. Blocks
 *       leakage / toxicity, and enforces a minimum-grounding floor using the turn's retrieval
 *       confidence (passed to the policy as {@code grounding_confidence}).</li>
 * </ul>
 *
 * <p>Unlike the memory harness, which <em>drops</em> disallowed content and degrades the
 * prompt, a guardrail block <em>fails the turn</em>: an injected prompt or a withheld answer
 * is a hard stop, surfaced as an {@link InputGuardrailViolationException} /
 * {@link OutputGuardrailViolationException}. When the decision engine is unreachable the
 * configured posture applies — <b>fail-closed</b> (default) blocks, <b>fail-open</b> passes.
 */
@Component
public class AgentGuardrailHarness {

    private static final Logger log = LoggerFactory.getLogger(AgentGuardrailHarness.class);

    private final GuardrailPolicyPort policy;
    private final GuardrailMetricsPort metrics;
    private final boolean failOpen;

    public AgentGuardrailHarness(
            GuardrailPolicyPort policy,
            GuardrailMetricsPort metrics,
            @Value("${agent.guardrail.fail-open:false}") boolean failOpen
    ) {
        this.policy = policy;
        this.metrics = metrics;
        this.failOpen = failOpen;
    }

    /**
     * Screen the inbound user message. Returns normally when the message may proceed.
     *
     * @throws InputGuardrailViolationException if the input is blocked by policy
     */
    public void guardInput(ConversationId conversationId, String userMessage) {

        GuardrailRequest request =
                GuardrailRequest.input(conversationId.toString(), userMessage, Map.of());

        GuardrailDecision decision = evaluate(request);
        if (!decision.allowed()) {
            log.info("Input guardrail blocked a message for conversation {}.", conversationId);
            throw new InputGuardrailViolationException(decision.reason());
        }
    }

    /**
     * Screen the outbound assistant answer. Returns normally when the answer may be returned.
     *
     * @param groundingConfidence the turn's RAG retrieval confidence (0–1), handed to the
     *                            policy so it can enforce a minimum-grounding floor
     * @throws OutputGuardrailViolationException if the answer is blocked by policy
     */
    public void guardOutput(ConversationId conversationId, String answer, double groundingConfidence) {

        GuardrailRequest request = GuardrailRequest.output(
                conversationId.toString(),
                answer,
                Map.of("grounding_confidence", groundingConfidence)
        );

        GuardrailDecision decision = evaluate(request);
        if (!decision.allowed()) {
            log.info("Output guardrail withheld an answer for conversation {}.", conversationId);
            throw new OutputGuardrailViolationException(decision.reason());
        }
    }

    private GuardrailDecision evaluate(GuardrailRequest request) {

        GuardrailDirection direction = request.direction();

        try {

            GuardrailDecision decision = policy.decide(request);
            metrics.recordDecision(direction, decision.allowed());

            if (!decision.allowed()) {
                log.debug("Guardrail {} blocked: {}", direction, decision.reason());
            }
            return decision;

        } catch (GuardrailPolicyUnavailableException e) {

            metrics.recordUnavailable(direction);
            log.warn(
                    "Guardrail policy engine unavailable for {}; falling back to fail-{}.",
                    direction,
                    failOpen ? "open (pass)" : "closed (block)",
                    e
            );
            return failOpen
                    ? GuardrailDecision.allow()
                    : GuardrailDecision.deny("guardrail policy engine unavailable (fail-closed)");
        }
    }
}
