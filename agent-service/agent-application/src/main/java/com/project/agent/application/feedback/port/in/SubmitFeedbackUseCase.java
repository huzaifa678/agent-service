package com.project.agent.application.feedback.port.in;

import com.project.agent.domain.feedback.Feedback;

/** Inbound port: submit new feedback for a message. */
public interface SubmitFeedbackUseCase {

    Feedback submit(SubmitFeedbackCommand command);
}
