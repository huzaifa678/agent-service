package com.project.agent.domain.execution.agent;

/** Lifecycle states for an {@link AgentExecution}. */
public enum AgentExecutionStatus {

    /** The LLM call is in progress; terminal transitions are still possible. */
    RUNNING,

    /** The LLM call finished successfully and token usage / cost were recorded. */
    COMPLETED,

    /** The LLM call ended with an error. */
    FAILED,

    /** The LLM call exceeded the allowed duration. */
    TIMEOUT
}
