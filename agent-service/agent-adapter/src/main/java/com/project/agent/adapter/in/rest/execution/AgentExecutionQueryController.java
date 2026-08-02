package com.project.agent.adapter.in.rest.execution;

import com.project.agent.adapter.in.rest.dto.AgentExecutionResponse;
import com.project.agent.adapter.in.rest.mapper.RestMapper;
import com.project.agent.application.execution.port.in.query.ExecutionQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the agent-execution read endpoints (get by id, list by conversation)
 * under {@code /api/agent/executions}.
 */
@RestController
@RequestMapping("/api/agent/executions")
@RequiredArgsConstructor
public class AgentExecutionQueryController {

    private final ExecutionQueries executionQueries;
    private final RestMapper mapper;

    @GetMapping("/{id}")
    public AgentExecutionResponse getById(
            @PathVariable UUID id
    ) {

        return mapper.toResponse(
                executionQueries.getById(id)
        );
    }

    @GetMapping(params = "conversationId")
    public List<AgentExecutionResponse> byConversation(
            @RequestParam UUID conversationId
    ) {

        return executionQueries
                .byConversation(conversationId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
