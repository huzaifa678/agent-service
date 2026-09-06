package com.project.agent.application.execution.port.out.memory;

/**
 * The two boundaries at which the harness governs the agent's long-term memory (the
 * pgvector conversation store).
 */
public enum MemoryOperation {

    /** Reading remembered passages back into the prompt. */
    RECALL,

    /** Committing an assistant message into long-term memory. */
    PERSIST
}
