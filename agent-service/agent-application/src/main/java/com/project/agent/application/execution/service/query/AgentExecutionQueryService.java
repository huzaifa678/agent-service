package com.project.agent.application.execution.service.query;

import com.project.agent.application.execution.exception.AgentExecutionNotFoundException;
import com.project.agent.application.execution.port.in.query.ExecutionQueries;
import com.project.agent.application.execution.port.out.conversation.AgentExecutionRepositoryPort;
import com.project.agent.domain.execution.agent.AgentExecution;
import com.project.agent.domain.vo.identity.AgentExecutionId;
import com.project.agent.domain.vo.identity.ConversationId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Read side of the execution context. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AgentExecutionQueryService implements ExecutionQueries {

    private final AgentExecutionRepositoryPort agentExecutionRepository;

    @Override
    public AgentExecution getById(UUID executionId) {
        AgentExecutionId id = AgentExecutionId.of(executionId);
        return agentExecutionRepository.findById(id)
                .orElseThrow(() -> AgentExecutionNotFoundException.of(id.toString()));
    }

    @Override
    public List<AgentExecution> byConversation(UUID conversationId) {
        return agentExecutionRepository.findByConversationId(ConversationId.of(conversationId));
    }
}
