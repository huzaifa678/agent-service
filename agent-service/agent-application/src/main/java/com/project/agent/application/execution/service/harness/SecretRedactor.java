package com.project.agent.application.execution.service.harness;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Masks credential-shaped substrings in text.
 *
 * <p>Long-term memory keeps secrets out by <em>dropping</em> whole passages (the memory policy,
 * {@link AgentMemoryHarness}). Short-term memory can't do that — it's the live transcript, and
 * the model needs the turn in front of it — so instead of dropping the message it redacts the
 * secret inside it. A user who pastes an API key still gets an answer; the key just doesn't get
 * echoed back through the prompt (and, on the next turn, through long-term recall).
 *
 * <p>The patterns mirror the deny rules in {@code policy/agent/memory.rego} — deliberately a
 * pragmatic starter set (common token shapes, private-key blocks, inline {@code key=value}
 * secrets), not a full DLP classifier.
 */
@Component
public class SecretRedactor {

    private record Rule(Pattern pattern, String replacement) {}

    private static final String MASK = "[REDACTED]";

    private final boolean enabled;
    private final List<Rule> rules;

    public SecretRedactor(
            @Value("${agent.memory.short-term.redact.enabled:true}") boolean enabled
    ) {
        this.enabled = enabled;
        this.rules = List.of(
                // Inline secrets — keep the label, mask the value: `password=hunter2` → `password=[REDACTED]`.
                new Rule(
                        Pattern.compile(
                                "(?i)((?:password|passwd|secret|api[_-]?key|access[_-]?key|token)\\s*[:=]\\s*)\\S+"),
                        "$1" + MASK),
                // Provider token shapes.
                new Rule(Pattern.compile("sk-[A-Za-z0-9]{16,}"), MASK),
                new Rule(Pattern.compile("xox[baprs]-[0-9A-Za-z-]{10,}"), MASK),
                // PEM private-key blocks.
                new Rule(
                        Pattern.compile(
                                "-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
                        "[REDACTED PRIVATE KEY]")
        );
    }

    /** Return {@code content} with any credential-shaped substrings masked. */
    public String redact(String content) {
        if (!enabled || content == null || content.isBlank()) {
            return content;
        }
        String out = content;
        for (Rule rule : rules) {
            out = rule.pattern().matcher(out).replaceAll(rule.replacement());
        }
        return out;
    }
}
