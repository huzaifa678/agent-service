package com.project.agent.application.execution.port.out.guardrail;

/**
 * The two boundaries at which the harness screens model I/O.
 */
public enum GuardrailDirection {

    /** The user's message on the way in — prompt-injection / jailbreak screening. */
    INPUT,

    /** The assistant's answer on the way out — leakage, toxicity, and grounding screening. */
    OUTPUT
}
