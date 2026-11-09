package com.project.agent.adapter.out.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.agent.application.execution.port.out.tool.ToolAccessRequest;
import com.project.agent.application.execution.port.out.tool.ToolDecision;
import com.project.agent.application.execution.port.out.tool.ToolPolicyPort;
import com.project.agent.application.execution.port.out.tool.ToolPolicyUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/**
 * {@link ToolPolicyPort} backed by Open Policy Agent. Active only when
 * {@code agent.tools.policy.enabled=true}; otherwise {@link AllowAllToolPolicyAdapter} stands
 * in, so local/dev runs need no OPA sidecar.
 *
 * <p>Each request is POSTed to the OPA Data API at {@code /v1/data/agent/tools/decision}; the
 * loaded {@code agent/tools} Rego bundle returns {@code {"allow": bool, "reason": str}}. Any
 * transport error, timeout, or missing/blank result becomes a
 * {@link ToolPolicyUnavailableException} — the adapter never invents a verdict. The call is
 * guarded by a Resilience4j {@code @CircuitBreaker} ({@code toolPolicy}); the fallback
 * surfaces the same exception, and the
 * {@link com.project.agent.application.execution.service.harness.AgentToolHarness} owns the
 * fail-open / fail-closed response.
 */
@Component
@ConditionalOnProperty(name = "agent.tools.policy.enabled", havingValue = "true")
public class OpaToolPolicyAdapter implements ToolPolicyPort {

    private final RestClient restClient;
    private final String decisionPath;

    public OpaToolPolicyAdapter(
            @Value("${agent.tools.policy.opa.base-url:http://localhost:8181}") String baseUrl,
            @Value("${agent.tools.policy.opa.decision-path:/v1/data/agent/tools/decision}") String decisionPath,
            @Value("${agent.tools.policy.opa.timeout-ms:250}") long timeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        this.decisionPath = decisionPath;
    }

    @Override
    @CircuitBreaker(name = "toolPolicy", fallbackMethod = "decideFallback")
    public ToolDecision decide(ToolAccessRequest request) {

        OpaResponse response;
        try {

            response = restClient.post()
                    .uri(decisionPath)
                    .body(new OpaQuery(toInput(request)))
                    .retrieve()
                    .body(OpaResponse.class);

        } catch (RestClientException e) {
            throw new ToolPolicyUnavailableException(
                    "OPA tool policy evaluation failed for tool '" + request.toolName() + "'", e);
        }

        if (response == null || response.result() == null) {
            throw new ToolPolicyUnavailableException(
                    "OPA returned no decision for tool '" + request.toolName() + "'");
        }

        OpaResult result = response.result();
        return result.allow()
                ? ToolDecision.allow()
                : ToolDecision.deny(result.reason() == null ? "denied by policy" : result.reason());
    }

    @SuppressWarnings("unused")
    private ToolDecision decideFallback(ToolAccessRequest request, Throwable t) {
        if (t instanceof ToolPolicyUnavailableException unavailable) {
            throw unavailable;
        }
        throw new ToolPolicyUnavailableException(
                "OPA tool policy circuit open for tool '" + request.toolName() + "'", t);
    }

    private Map<String, Object> toInput(ToolAccessRequest request) {
        return Map.of(
                "tool", request.toolName(),
                "arguments", request.arguments() == null ? "" : request.arguments(),
                "attributes", request.attributes()
        );
    }

    /** OPA Data API request envelope: {@code {"input": {...}}}. */
    private record OpaQuery(Map<String, Object> input) {}

    /** OPA Data API response envelope: {@code {"result": {...}}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpaResponse(OpaResult result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpaResult(boolean allow, String reason) {}
}
