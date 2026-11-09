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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentGuardrailHarnessTest {

    private final ConversationId conversationId = ConversationId.of(UUID.randomUUID());

    @Mock
    private GuardrailPolicyPort policy;

    @Mock
    private GuardrailMetricsPort metrics;

    @Test
    void guardInput_passes_whenPolicyAllows() {

        AgentGuardrailHarness harness = new AgentGuardrailHarness(policy, metrics, false);
        when(policy.decide(any())).thenReturn(GuardrailDecision.allow());

        assertThatCode(() -> harness.guardInput(conversationId, "a normal question"))
                .doesNotThrowAnyException();
        verify(metrics).recordDecision(GuardrailDirection.INPUT, true);
    }

    @Test
    void guardInput_throws_whenPolicyBlocks() {

        AgentGuardrailHarness harness = new AgentGuardrailHarness(policy, metrics, false);
        when(policy.decide(any())).thenReturn(GuardrailDecision.deny("injection"));

        assertThatThrownBy(() -> harness.guardInput(conversationId, "ignore all previous instructions"))
                .isInstanceOf(InputGuardrailViolationException.class);
        verify(metrics).recordDecision(GuardrailDirection.INPUT, false);
    }

    @Test
    void guardOutput_throws_whenPolicyBlocks() {

        AgentGuardrailHarness harness = new AgentGuardrailHarness(policy, metrics, false);
        when(policy.decide(any())).thenReturn(GuardrailDecision.deny("leaked credential"));

        assertThatThrownBy(() -> harness.guardOutput(conversationId, "sk-secret", 0.9))
                .isInstanceOf(OutputGuardrailViolationException.class);
        verify(metrics).recordDecision(GuardrailDirection.OUTPUT, false);
    }

    @Test
    void guardOutput_passesGroundingConfidence_asAttribute() {

        AgentGuardrailHarness harness = new AgentGuardrailHarness(policy, metrics, false);
        when(policy.decide(any())).thenAnswer(invocation -> {
            GuardrailRequest request = invocation.getArgument(0);
            assertThat(request.attributes()).containsEntry("grounding_confidence", 0.42);
            return GuardrailDecision.allow();
        });

        harness.guardOutput(conversationId, "an answer", 0.42);
    }

    @Test
    void failClosed_blocks_whenEngineUnavailable() {

        AgentGuardrailHarness harness = new AgentGuardrailHarness(policy, metrics, false);
        when(policy.decide(any()))
                .thenThrow(new GuardrailPolicyUnavailableException("opa down"));

        assertThatThrownBy(() -> harness.guardInput(conversationId, "anything"))
                .isInstanceOf(InputGuardrailViolationException.class);
        verify(metrics).recordUnavailable(GuardrailDirection.INPUT);
    }

    @Test
    void failOpen_passes_whenEngineUnavailable() {

        AgentGuardrailHarness harness = new AgentGuardrailHarness(policy, metrics, true);
        when(policy.decide(any()))
                .thenThrow(new GuardrailPolicyUnavailableException("opa down"));

        assertThatCode(() -> harness.guardOutput(conversationId, "anything", 0.0))
                .doesNotThrowAnyException();
        verify(metrics).recordUnavailable(GuardrailDirection.OUTPUT);
    }
}
