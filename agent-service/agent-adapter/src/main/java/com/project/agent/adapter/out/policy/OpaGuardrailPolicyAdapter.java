package com.project.agent.adapter.out.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.agent.application.execution.port.out.guardrail.GuardrailDecision;
import com.project.agent.application.execution.port.out.guardrail.GuardrailPolicyPort;
import com.project.agent.application.execution.port.out.guardrail.GuardrailPolicyUnavailableException;
import com.project.agent.application.execution.port.out.guardrail.GuardrailRequest;
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
 * {@link GuardrailPolicyPort} backed by Open Policy Agent. Active only when
 * {@code agent.guardrail.enabled=true}; otherwise {@link AllowAllGuardrailPolicyAdapter}
 * stands in, so local/dev runs need no OPA sidecar.
 *
 * <p>Each request is POSTed to the OPA Data API at {@code /v1/data/agent/io/decision}; the
 * loaded {@code agent/io} Rego bundle returns {@code {"allow": bool, "reason": str}}. Any
 * transport error, timeout, or missing/blank result becomes a
 * {@link GuardrailPolicyUnavailableException} — the adapter never invents a verdict. The call
 * is guarded by a Resilience4j {@code @CircuitBreaker} ({@code guardrailPolicy}); the fallback
 * surfaces the same exception, and the
 * {@link com.project.agent.application.execution.service.harness.AgentGuardrailHarness} owns
 * the fail-open / fail-closed response.
 */
@Component
@ConditionalOnProperty(name = "agent.guardrail.enabled", havingValue = "true")
public class OpaGuardrailPolicyAdapter implements GuardrailPolicyPort {

    private final RestClient restClient;
    private final String decisionPath;

    public OpaGuardrailPolicyAdapter(
            @Value("${agent.guardrail.opa.base-url:http://localhost:8181}") String baseUrl,
            @Value("${agent.guardrail.opa.decision-path:/v1/data/agent/io/decision}") String decisionPath,
            @Value("${agent.guardrail.opa.timeout-ms:250}") long timeoutMs
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
    @CircuitBreaker(name = "guardrailPolicy", fallbackMethod = "decideFallback")
    public GuardrailDecision decide(GuardrailRequest request) {

        OpaResponse response;
        try {

            response = restClient.post()
                    .uri(decisionPath)
                    .body(new OpaQuery(toInput(request)))
                    .retrieve()
                    .body(OpaResponse.class);

        } catch (RestClientException e) {
            throw new GuardrailPolicyUnavailableException(
                    "OPA guardrail evaluation failed for a " + request.direction() + " check", e);
        }

        if (response == null || response.result() == null) {
            throw new GuardrailPolicyUnavailableException(
                    "OPA returned no decision for a " + request.direction() + " check");
        }

        OpaResult result = response.result();
        return result.allow()
                ? GuardrailDecision.allow()
                : GuardrailDecision.deny(result.reason() == null ? "blocked by policy" : result.reason());
    }

    @SuppressWarnings("unused")
    private GuardrailDecision decideFallback(GuardrailRequest request, Throwable t) {
        if (t instanceof GuardrailPolicyUnavailableException unavailable) {
            throw unavailable;
        }
        throw new GuardrailPolicyUnavailableException(
                "OPA guardrail circuit open for a " + request.direction() + " check", t);
    }

    private Map<String, Object> toInput(GuardrailRequest request) {
        return Map.of(
                "direction", request.direction().name(),
                "conversation_id", request.conversationId(),
                "content", request.content() == null ? "" : request.content(),
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
