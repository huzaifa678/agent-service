package com.project.agent.application.execution.service.harness;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;

import java.util.List;

/**
 * The working context assembled for one agent turn: the full prompt (system prompt, then the
 * short-term window, then any long-term passages that were recalled and allowed) together with
 * the long-term retrieval confidence (0–1) recorded on the execution.
 */
public record MemoryRecall(
        List<ChatMessage> context,
        double retrievalConfidence
) {
}
