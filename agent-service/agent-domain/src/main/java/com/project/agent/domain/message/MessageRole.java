package com.project.agent.domain.message;

/** The sender role of a {@link Message}, mirroring the LLM chat-completion contract. */
public enum MessageRole {
    /** End-user input. */
    USER,
    /** LLM-generated response. */
    ASSISTANT,
    /** Injected system instruction (not visible to the user). */
    SYSTEM,
    /** Tool/function call result returned to the model. */
    TOOL
}
