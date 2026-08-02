package com.project.agent.adapter.in.rest.conversation;

import com.project.agent.adapter.in.rest.dto.ConversationResponse;
import com.project.agent.adapter.in.rest.dto.MessageResponse;
import com.project.agent.adapter.in.rest.mapper.RestMapper;
import com.project.agent.application.conversation.port.in.ConversationQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the conversation read endpoints (get by id, list by user, list
 * messages) under {@code /api/agent/conversations}.
 */
@RestController
@RequestMapping("/api/agent/conversations")
@RequiredArgsConstructor
public class ConversationQueryController {

    private final ConversationQueries conversationQueries;

    private final RestMapper mapper;

    @GetMapping("/{id}")
    public ConversationResponse get(
            @PathVariable UUID id
    ) {

        return mapper.toResponse(
                conversationQueries.getById(id)
        );
    }

    @GetMapping(params = "userId")
    public List<ConversationResponse> byUser(
            @RequestParam UUID userId
    ) {

        return conversationQueries
                .byUser(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(
            @PathVariable UUID id
    ) {

        return conversationQueries
                .messages(id)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
