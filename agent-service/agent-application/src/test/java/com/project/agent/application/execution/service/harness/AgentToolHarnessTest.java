package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.tool.ToolAccessRequest;
import com.project.agent.application.execution.port.out.tool.ToolDecision;
import com.project.agent.application.execution.port.out.tool.ToolPolicyMetricsPort;
import com.project.agent.application.execution.port.out.tool.ToolPolicyPort;
import com.project.agent.application.execution.port.out.tool.ToolPolicyUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentToolHarnessTest {

    @Mock
    private ToolPolicyPort policy;

    @Mock
    private ToolPolicyMetricsPort metrics;

    @Test
    void authorizeInvocation_returnsTrue_whenPolicyAllows() {

        AgentToolHarness harness = new AgentToolHarness(policy, metrics, false);
        when(policy.decide(any())).thenReturn(ToolDecision.allow());

        assertThat(harness.authorizeInvocation("web_search", "{\"q\":\"x\"}")).isTrue();
        verify(metrics).recordDecision("web_search", true);
    }

    @Test
    void authorizeInvocation_returnsFalse_whenPolicyDenies() {

        AgentToolHarness harness = new AgentToolHarness(policy, metrics, false);
        when(policy.decide(any())).thenReturn(ToolDecision.deny("ssrf target"));

        assertThat(harness.authorizeInvocation("web_search", "http://localhost")).isFalse();
        verify(metrics).recordDecision("web_search", false);
    }

    @Test
    void authorizeEnablement_sendsEnablementRequest_withEmptyArguments() {

        AgentToolHarness harness = new AgentToolHarness(policy, metrics, false);
        when(policy.decide(any())).thenAnswer(invocation -> {
            ToolAccessRequest request = invocation.getArgument(0);
            assertThat(request.arguments()).isEmpty();
            return ToolDecision.allow();
        });

        assertThat(harness.authorizeEnablement("calculator")).isTrue();
        verify(metrics).recordDecision("calculator", true);
    }

    @Test
    void failClosed_deniesTool_whenEngineUnavailable() {

        AgentToolHarness harness = new AgentToolHarness(policy, metrics, false);
        when(policy.decide(any()))
                .thenThrow(new ToolPolicyUnavailableException("opa down"));

        assertThat(harness.authorizeInvocation("code_interpreter", "rm -rf /")).isFalse();
        verify(metrics).recordUnavailable(eq("code_interpreter"));
    }

    @Test
    void failOpen_allowsTool_whenEngineUnavailable() {

        AgentToolHarness harness = new AgentToolHarness(policy, metrics, true);
        when(policy.decide(any()))
                .thenThrow(new ToolPolicyUnavailableException("opa down"));

        assertThat(harness.authorizeInvocation("web_search", "{}")).isTrue();
        verify(metrics).recordUnavailable(eq("web_search"));
    }
}
