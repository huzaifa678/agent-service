package com.project.agent.application.execution.exception;

import com.project.agent.application.shared.exception.ResourceNotFoundException;

/** Application-layer error: no {@code AgentExecution} exists for the requested id. */
public class AgentExecutionNotFoundException extends ResourceNotFoundException {

    public AgentExecutionNotFoundException(String message) {
        super(message);
    }

    public static AgentExecutionNotFoundException of(String id) {
        return new AgentExecutionNotFoundException("Agent execution not found: " + id);
    }
}
