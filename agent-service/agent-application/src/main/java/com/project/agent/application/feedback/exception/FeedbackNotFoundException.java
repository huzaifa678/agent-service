package com.project.agent.application.feedback.exception;

import com.project.agent.application.shared.exception.ResourceNotFoundException;

/** Application-layer error: no {@code Feedback} exists for the requested id. */
public class FeedbackNotFoundException extends ResourceNotFoundException {

    public FeedbackNotFoundException(String message) {
        super(message);
    }

    public static FeedbackNotFoundException of(String id) {
        return new FeedbackNotFoundException("Feedback not found: " + id);
    }
}
