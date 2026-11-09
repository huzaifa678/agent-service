package com.project.agent.application.execution.exception;

import com.project.agent.application.shared.exception.ApplicationException;

/**
 * Raised when a subject (conversation today; tenant once {@code TenantId} is threaded
 * end-to-end) has exhausted its configured token or cost budget. The turn is refused before
 * the LLM is called, capping runaway spend from an agent loop or an abusive client.
 */
public class BudgetExceededException extends ApplicationException {

    public BudgetExceededException(String reason) {
        super("Budget exceeded: " + reason);
    }
}
