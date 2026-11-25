package com.project.agent.application.execution.service.command;

import com.project.agent.application.execution.exception.IdempotencyConflictException;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.in.usecase.RunAgentUseCase;
import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.application.execution.port.out.idempotency.IdempotencyStorePort;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.application.execution.service.workflow.AgentExecutionWorkflow;
import com.project.agent.application.execution.service.command.commands.*;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Synchronous application service.
 *
 * <p>Coordinates the execution workflow and delegates all preparation and
 * persistence responsibilities to {@link AgentExecutionWorkflow}.
 *
 * <p>When the request carries an {@code Idempotency-Key}, the run is guarded so a retry replays the
 * original execution instead of spending on the LLM again: the key is reserved before the LLM call
 * (so a concurrent retry is rejected) and completed with the resulting execution id afterwards.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AgentExecutionCommandService implements RunAgentUseCase {

    /** A reservation older than this is treated as abandoned (crashed run) and may be reclaimed. */
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(5);

    private final AgentExecutionWorkflow workflow;

    private final LlmInvocationService llmInvocationService;

    private final IdempotencyStorePort idempotencyStore;

    private final AgentExecutionRepositoryPort agentExecutionRepository;

    @Override
    public AgentExecution run(RunAgentCommand command) {
        String key = command.idempotencyKey();
        if (key == null || key.isBlank()) {
            return execute(command); // no key: current behaviour, no dedup
        }

        String fingerprint = fingerprint(command);

        Optional<IdempotencyStorePort.Entry> existing = idempotencyStore.find(key);
        if (existing.isPresent()) {
            return replay(existing.get(), fingerprint);
        }

        if (!idempotencyStore.reserve(key, fingerprint, RESERVATION_TTL)) {
            // Lost the race between find and reserve: another request owns the key. Re-read and
            // replay if it has already completed, otherwise report it as in progress.
            return idempotencyStore.find(key)
                    .map(entry -> replay(entry, fingerprint))
                    .orElseThrow(() -> new IdempotencyConflictException(
                            "A request with this Idempotency-Key is already in progress"));
        }

        try {
            AgentExecution execution = execute(command);
            idempotencyStore.complete(key, execution.getId().value());
            return execution;
        } catch (RuntimeException e) {
            // Free the reservation so the caller can retry (the LLM spend did not complete).
            idempotencyStore.release(key);
            throw e;
        }
    }

    /** Replays a stored result, or rejects a still-running / mismatched key. */
    private AgentExecution replay(IdempotencyStorePort.Entry entry, String fingerprint) {
        if (!entry.fingerprint().equals(fingerprint)) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key was reused with a different request");
        }
        if (entry.status() != IdempotencyStorePort.Status.COMPLETED || entry.resultId() == null) {
            throw new IdempotencyConflictException(
                    "A request with this Idempotency-Key is already in progress");
        }
        return agentExecutionRepository.findById(AgentExecutionId.of(entry.resultId()))
                .orElseThrow(() -> new IdempotencyConflictException(
                        "The idempotent result is no longer available; retry with a new key"));
    }

    private AgentExecution execute(RunAgentCommand command) {

        AgentExecutionContext context =
                workflow.prepare(command);

        ChatResult result =
                llmInvocationService.invoke(
                        context.execution(),
                        context.model(),
                        context.provider(),
                        context.prompt(),
                        context.enabledTools()
                );

        return workflow.finish(
                context,
                result
        );
    }

    /** Stable hash of the request, so a replay of a reused key with a changed body is rejected. */
    private static String fingerprint(RunAgentCommand command) {
        String canonical = String.join("\n",
                String.valueOf(command.conversationId()),
                command.userMessage(),
                command.modelName(),
                command.providerName(),
                String.join(",", command.enabledTools() == null ? List.of() : command.enabledTools()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
