package com.project.agent.application.execution.port.out.llm.port;

import com.project.agent.application.execution.port.out.llm.model.ChatRequest;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;

/**
 * Outbound port abstracting the LLM provider (implemented by a langchain4j adapter).
 * Keeps all langchain4j types out of the application layer.
 *
 * <p>Implementations signal failures with the domain model exceptions so the
 * orchestrator can map them to the {@link com.project.agent.domain.execution.agent.AgentExecution}
 * state machine:
 * <ul>
 *   <li>{@link com.project.agent.domain.execution.agent.exception.ModelTimeoutException}</li>
 *   <li>{@link com.project.agent.domain.execution.agent.exception.ModelRateLimitException}</li>
 *   <li>{@link com.project.agent.domain.execution.agent.exception.ModelUnavailableException}</li>
 *   <li>{@link com.project.agent.domain.execution.agent.exception.ModelAuthenticationException}</li>
 *   <li>{@link com.project.agent.domain.execution.agent.exception.ModelProviderException}</li>
 * </ul>
 */
public interface ChatModelPort {

    ChatResult generate(ChatRequest request);
}
