package com.project.agent.application.execution.exception;

import com.project.agent.application.shared.exception.ApplicationException;

/**
 * Raised when the assistant's answer is blocked by the output guardrail policy (e.g. it leaks
 * a credential, is flagged for toxicity, or is insufficiently grounded given the turn's
 * retrieval confidence). The answer is withheld and not persisted to the conversation or
 * long-term memory.
 */
public class OutputGuardrailViolationException extends ApplicationException {

    public OutputGuardrailViolationException(String reason) {
        super("Answer withheld by guardrail policy: " + reason);
    }
}
