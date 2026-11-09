package com.project.agent.application.execution.exception;

import com.project.agent.application.shared.exception.ApplicationException;

/**
 * Raised when the inbound user message is blocked by the input guardrail policy (e.g. a
 * detected prompt-injection or jailbreak attempt). The turn is refused before any LLM or
 * memory work, so the suspicious input never reaches the model or long-term memory.
 */
public class InputGuardrailViolationException extends ApplicationException {

    public InputGuardrailViolationException(String reason) {
        super("Input blocked by guardrail policy: " + reason);
    }
}
