package com.project.agent.adapter.in.rest.feedback;

import com.project.agent.adapter.in.rest.dto.FeedbackResponse;
import com.project.agent.adapter.in.rest.dto.SubmitFeedbackRequest;
import com.project.agent.adapter.in.rest.dto.UpdateFeedbackRequest;
import com.project.agent.adapter.in.rest.mapper.RestMapper;
import com.project.agent.application.feedback.port.in.SubmitFeedbackCommand;
import com.project.agent.application.feedback.port.in.SubmitFeedbackUseCase;
import com.project.agent.application.feedback.port.in.UpdateFeedbackCommand;
import com.project.agent.application.feedback.port.in.UpdateFeedbackUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST adapter for the feedback command endpoints (submit, update) under
 * {@code /api/agent/feedback}.
 */
@RestController
@RequestMapping("/api/agent/feedback")
@RequiredArgsConstructor
public class FeedbackCommandController {

    private final SubmitFeedbackUseCase submitFeedbackUseCase;

    private final UpdateFeedbackUseCase updateFeedbackUseCase;

    private final RestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(
            @Valid @RequestBody SubmitFeedbackRequest request
    ) {

        return mapper.toResponse(
                submitFeedbackUseCase.submit(
                        new SubmitFeedbackCommand(
                                request.conversationId(),
                                request.messageId(),
                                request.rating(),
                                request.comment()
                        )
                )
        );
    }

    @PatchMapping("/{id}")
    public FeedbackResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeedbackRequest request
    ) {

        return mapper.toResponse(
                updateFeedbackUseCase.update(
                        new UpdateFeedbackCommand(
                                id,
                                request.rating(),
                                request.comment()
                        )
                )
        );
    }
}
