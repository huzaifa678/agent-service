package com.project.agent.application.execution.service.command;

import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.in.usecase.RunAgentUseCase;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import com.project.agent.application.execution.service.workflow.AgentExecutionWorkflow;
import com.project.agent.application.execution.service.command.commands.*;
import com.project.agent.domain.execution.agent.AgentExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synchronous application service.
 *
 * <p>Coordinates the execution workflow and delegates all preparation and
 * persistence responsibilities to {@link AgentExecutionWorkflow}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AgentExecutionCommandService implements RunAgentUseCase {

    private final AgentExecutionWorkflow workflow;

    private final LlmInvocationService llmInvocationService;

    @Override
    public AgentExecution run(RunAgentCommand command) {

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
}
