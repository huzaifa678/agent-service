package com.project.agent.adapter.in.rest.feedback;

import com.project.agent.adapter.in.rest.dto.FeedbackResponse;
import com.project.agent.adapter.in.rest.mapper.RestMapper;
import com.project.agent.application.feedback.port.in.FeedbackQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the feedback read endpoints (get by id, list by conversation) under
 * {@code /api/agent/feedback}.
 */
@RestController
@RequestMapping("/api/agent/feedback")
@RequiredArgsConstructor
public class FeedbackQueryController {

    private final FeedbackQueries feedbackQueries;

    private final RestMapper mapper;

    @GetMapping("/{id}")
    public FeedbackResponse get(
            @PathVariable UUID id
    ) {

        return mapper.toResponse(
                feedbackQueries.getById(id)
        );
    }

    @GetMapping(params = "conversationId")
    public List<FeedbackResponse> byConversation(
            @RequestParam UUID conversationId
    ) {

        return feedbackQueries
                .byConversation(conversationId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
