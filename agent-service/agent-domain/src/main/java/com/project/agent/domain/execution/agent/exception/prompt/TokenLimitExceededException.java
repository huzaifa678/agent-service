package com.project.agent.domain.execution.agent.exception;

import com.project.agent.domain.exception.BusinessRuleViolationException;

/**
 * Raised when a request's token count exceeds the configured limit for the model.
 */
public class TokenLimitExceededException extends BusinessRuleViolationException {

    public TokenLimitExceededException() {
        super("Token limit exceeded.");
    }

    public TokenLimitExceededException(
            int requested,
            int maximum
    ) {
        super(
                "Requested " + requested +
                        " tokens but maximum allowed is " +
                        maximum + "."
        );
    }
}
