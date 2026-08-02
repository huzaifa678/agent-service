package com.project.agent.application.execution.service.streaming;

import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.billing.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StreamingResponseAssemblerTest {

    @Test
    void build_returnsEmptyResultInitially() {

        StreamingResponseAssembler assembler =
                new StreamingResponseAssembler();

        ChatResult result = assembler.build();

        assertEquals("", result.content());
        assertEquals(TokenUsage.empty(), result.tokenUsage());
        assertNull(result.cost());
        assertTrue(result.toolCalls().isEmpty());
    }

    @Test
    void append_accumulatesTokens() {

        StreamingResponseAssembler assembler =
                new StreamingResponseAssembler();

        assembler.append("Hello");
        assembler.append(" ");
        assembler.append("World");

        ChatResult result = assembler.build();

        assertEquals(
                "Hello World",
                result.content()
        );
    }

    @Test
    void append_nullToken_isIgnored() {

        StreamingResponseAssembler assembler =
                new StreamingResponseAssembler();

        assembler.append(null);
        assembler.append("Hi");

        assertEquals(
                "Hi",
                assembler.build().content()
        );
    }

    @Test
    void addToolCall_addsToolCalls() {

        StreamingResponseAssembler assembler =
                new StreamingResponseAssembler();

        ToolCall tool =
                new ToolCall(
                        "calculator",
                        "{}"
                );

        assembler.addToolCall(tool);

        ChatResult result = assembler.build();

        assertEquals(1, result.toolCalls().size());
        assertEquals(tool, result.toolCalls().getFirst());
    }

    @Test
    void tokenUsage_setsUsage() {

        StreamingResponseAssembler assembler =
                new StreamingResponseAssembler();

        TokenUsage usage =
                TokenUsage.of(12, 7);

        assembler.tokenUsage(usage);

        assertEquals(
                usage,
                assembler.build().tokenUsage()
        );
    }

    @Test
    void cost_setsCost() {

        StreamingResponseAssembler assembler =
                new StreamingResponseAssembler();

        Money cost =
                Money.of(
                        BigDecimal.ONE,
                        Currency.getInstance("USD")
                );

        assembler.cost(cost);

        assertEquals(
                cost,
                assembler.build().cost()
        );
    }
}
