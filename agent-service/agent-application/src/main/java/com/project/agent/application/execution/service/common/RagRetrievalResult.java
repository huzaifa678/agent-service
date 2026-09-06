package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;

import java.util.List;

/**
 * Outcome of a RAG retrieval: the SYSTEM context messages to inject into the prompt
 * (kept passages plus a grounding header, or a grounding guard when none were kept) and
 * the retrieval confidence (0–1) — the strongest kept relevance score, or {@code 0} when
 * nothing cleared the bar. The confidence is persisted on the execution and emitted as a
 * metric.
 */
public record RagRetrievalResult(
        List<ChatMessage> contextMessages,
        double confidence
) {
}
