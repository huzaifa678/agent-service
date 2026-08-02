package com.project.agent.application.feedback.port.in;

import com.project.agent.domain.feedback.Feedback;

/** Inbound port: amend an existing feedback record. */
public interface UpdateFeedbackUseCase {

    Feedback update(UpdateFeedbackCommand command);
}
