package com.project.agent.application.execution.port.in.usecase;

import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.domain.execution.agent.AgentExecution;

/** Inbound port: run one agent turn and return the recorded execution. */
public interface RunAgentUseCase {

    AgentExecution run(RunAgentCommand command);
}
