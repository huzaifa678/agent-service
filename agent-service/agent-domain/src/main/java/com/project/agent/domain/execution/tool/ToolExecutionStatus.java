package com.project.agent.domain.execution.tool;

/** Lifecycle states for a {@link ToolExecution}. */
public enum ToolExecutionStatus {

    /** The tool call has been dispatched and is awaiting a response. */
    RUNNING,

    /** The tool returned a response successfully. */
    COMPLETED,

    /** The tool call ended with an error. */
    FAILED
}
