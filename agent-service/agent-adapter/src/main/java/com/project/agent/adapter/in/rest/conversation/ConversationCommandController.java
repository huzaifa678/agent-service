package com.project.agent.adapter.in.rest.conversation;

import com.project.agent.adapter.in.rest.dto.AddMessageRequest;
import com.project.agent.adapter.in.rest.dto.ConversationResponse;
import com.project.agent.adapter.in.rest.dto.RenameConversationRequest;
import com.project.agent.adapter.in.rest.dto.StartConversationRequest;
import com.project.agent.adapter.in.rest.mapper.RestMapper;
import com.project.agent.application.conversation.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST adapter for the conversation command endpoints (start, rename, add message,
 * archive, delete) under {@code /api/agent/conversations}. Delegates to the command
 * use cases and maps aggregates to response DTOs.
 */
@RestController
@RequestMapping("/api/agent/conversations")
@RequiredArgsConstructor
public class ConversationCommandController {

    private final StartConversationUseCase startConversationUseCase;

    private final RenameConversationUseCase renameConversationUseCase;

    private final AddMessageUseCase addMessageUseCase;

    private final ArchiveConversationUseCase archiveConversationUseCase;

    private final DeleteConversationUseCase deleteConversationUseCase;

    private final RestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse start(
            @Valid @RequestBody StartConversationRequest request
    ) {

        return mapper.toResponse(
                startConversationUseCase.start(
                        new StartConversationCommand(
                                request.tenantId(),
                                request.userId(),
                                request.title()
                        )
                )
        );
    }

    @PostMapping("/{id}/messages")
    public ConversationResponse addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddMessageRequest request
    ) {

        return mapper.toResponse(
                addMessageUseCase.addMessage(
                        new AddMessageCommand(
                                id,
                                request.role(),
                                request.content(),
                                request.promptTokens(),
                                request.completionTokens()
                        )
                )
        );
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(
            @PathVariable UUID id,
            @Valid @RequestBody RenameConversationRequest request
    ) {

        renameConversationUseCase.rename(
                new RenameConversationCommand(
                        id,
                        request.title()
                )
        );
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable UUID id
    ) {

        archiveConversationUseCase.archive(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id
    ) {

        deleteConversationUseCase.delete(id);
    }
}
