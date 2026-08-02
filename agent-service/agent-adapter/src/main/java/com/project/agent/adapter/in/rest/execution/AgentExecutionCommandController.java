package com.project.agent.adapter.in.rest.execution;

import com.project.agent.adapter.in.rest.dto.AgentExecutionResponse;
import com.project.agent.adapter.in.rest.dto.RunAgentRequest;
import com.project.agent.adapter.in.rest.mapper.RestMapper;
import com.project.agent.application.execution.port.in.command.RunAgentCommand;
import com.project.agent.application.execution.port.in.usecase.RunAgentUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST adapter for the agent-execution command endpoint (run agent) under
 * {@code /api/agent/executions}.
 */
@RestController
@RequestMapping("/api/agent/executions")
@RequiredArgsConstructor
public class AgentExecutionCommandController {

    private final RunAgentUseCase runAgentUseCase;
    private final RestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentExecutionResponse run(
            @Valid @RequestBody RunAgentRequest request
    ) {

        return mapper.toResponse(
                runAgentUseCase.run(
                        new RunAgentCommand(
                                request.conversationId(),
                                request.userMessage(),
                                request.modelName(),
                                request.providerName(),
                                request.enabledTools()
                        )
                )
        );
    }
}
