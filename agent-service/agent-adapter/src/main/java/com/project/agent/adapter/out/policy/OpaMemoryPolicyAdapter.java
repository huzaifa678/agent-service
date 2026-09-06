package com.project.agent.adapter.out.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.agent.application.execution.port.out.memory.MemoryAccessRequest;
import com.project.agent.application.execution.port.out.memory.MemoryDecision;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyPort;
import com.project.agent.application.execution.port.out.memory.MemoryPolicyUnavailableException;
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
 * {@link MemoryPolicyPort} backed by Open Policy Agent. Active only when
 * {@code agent.memory.policy.enabled=true}; otherwise {@link AllowAllMemoryPolicyAdapter}
 * stands in.
 *
 * <p>OPA runs as a sidecar in the pod (the same "policy as code" engine the repo already
 * uses to lint Dockerfiles in CI), so the call is a localhost round-trip on the hot path.
 * Each request is POSTed to the Data API at {@code /v1/data/agent/memory/decision}; the
 * loaded {@code agent/memory} Rego bundle returns {@code {"allow": bool, "reason": str}}.
 *
 * <p>Any transport error, timeout, or missing/blank result becomes a
 * {@link MemoryPolicyUnavailableException} — the adapter never invents a verdict. The call is
 * guarded by a Resilience4j {@code @CircuitBreaker} ({@code memoryPolicy}): a sustained OPA
 * outage trips the breaker so requests fail fast instead of each paying the timeout, and the
 * fallback surfaces the same {@link MemoryPolicyUnavailableException}. The
 * {@link com.project.agent.application.execution.service.harness.AgentMemoryHarness} owns the
 * fail-open / fail-closed response to that.
 */
@Component
@ConditionalOnProperty(name = "agent.memory.policy.enabled", havingValue = "true")
public class OpaMemoryPolicyAdapter implements MemoryPolicyPort {

    private final RestClient restClient;
    private final String decisionPath;

    public OpaMemoryPolicyAdapter(
            @Value("${agent.memory.policy.opa.base-url:http://localhost:8181}") String baseUrl,
            @Value("${agent.memory.policy.opa.decision-path:/v1/data/agent/memory/decision}") String decisionPath,
            @Value("${agent.memory.policy.opa.timeout-ms:250}") long timeoutMs
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
    @CircuitBreaker(name = "memoryPolicy", fallbackMethod = "decideFallback")
    public MemoryDecision decide(MemoryAccessRequest request) {

        OpaResponse response;
        try {

            response = restClient.post()
                    .uri(decisionPath)
                    .body(new OpaQuery(toInput(request)))
                    .retrieve()
                    .body(OpaResponse.class);

        } catch (RestClientException e) {
            throw new MemoryPolicyUnavailableException(
                    "OPA memory policy evaluation failed for a " + request.operation() + " access", e);
        }

        if (response == null || response.result() == null) {
            // An undefined decision means the policy bundle is not loaded as expected — treat
            // as "no verdict" rather than silently allowing.
            throw new MemoryPolicyUnavailableException(
                    "OPA returned no decision for a " + request.operation() + " access");
        }

        OpaResult result = response.result();
        return result.allow()
                ? MemoryDecision.allow()
                : MemoryDecision.deny(result.reason() == null ? "denied by policy" : result.reason());
    }

    /**
     * Resilience4j fallback. Invoked both when the {@code memoryPolicy} circuit is open
     * ({@code CallNotPermittedException}) and when {@link #decide} itself raised — in every
     * case the outcome is "no verdict", surfaced as {@link MemoryPolicyUnavailableException}
     * so the harness applies its configured fail-open / fail-closed posture.
     */
    @SuppressWarnings("unused")
    private MemoryDecision decideFallback(MemoryAccessRequest request, Throwable t) {
        if (t instanceof MemoryPolicyUnavailableException unavailable) {
            throw unavailable;
        }
        throw new MemoryPolicyUnavailableException(
                "OPA memory policy circuit open for a " + request.operation() + " access", t);
    }

    private Map<String, Object> toInput(MemoryAccessRequest request) {
        return Map.of(
                "operation", request.operation().name(),
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
