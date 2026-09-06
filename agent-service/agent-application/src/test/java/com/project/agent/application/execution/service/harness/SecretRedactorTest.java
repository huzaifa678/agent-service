package com.project.agent.application.execution.service.harness;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretRedactorTest {

    private final SecretRedactor redactor = new SecretRedactor(true);

    @Test
    void masksOpenAiStyleKey() {
        String out = redactor.redact("here is the key sk-abcd1234efgh5678ijkl to use");
        assertThat(out).doesNotContain("sk-abcd1234efgh5678ijkl").contains("[REDACTED]");
    }

    @Test
    void masksInlinePasswordButKeepsTheLabel() {
        String out = redactor.redact("log in with password=hunter2 then retry");
        assertThat(out).doesNotContain("hunter2");
        assertThat(out).contains("password=[REDACTED]");
    }

    @Test
    void masksPrivateKeyBlock() {
        String pem = "-----BEGIN RSA PRIVATE KEY-----\nMIIEuff...\n-----END RSA PRIVATE KEY-----";
        String out = redactor.redact("my key:\n" + pem);
        assertThat(out).doesNotContain("MIIEuff").contains("[REDACTED PRIVATE KEY]");
    }

    @Test
    void leavesOrdinaryTextUntouched() {
        String text = "Your invoice INV-4821 was paid on the 3rd.";
        assertThat(redactor.redact(text)).isEqualTo(text);
    }

    @Test
    void disabledRedactorIsAPassThrough() {
        SecretRedactor off = new SecretRedactor(false);
        String text = "token=sk-abcd1234efgh5678ijkl";
        assertThat(off.redact(text)).isEqualTo(text);
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(redactor.redact(null)).isNull();
        assertThat(redactor.redact("   ")).isEqualTo("   ");
    }
}
