package com.project.agent.adapter.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Both fields optional: a {@code null} leaves that attribute unchanged. */
public record UpdateFeedbackRequest(
        @Min(1) @Max(5) Integer rating,
        String comment
) {}
