package com.project.agent.adapter.out.llm.schema;

import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

import java.util.List;

/**
 * The single JSON schema every agent response is constrained to, on both the
 * synchronous and the streaming path. Providers that support strict structured
 * output (OpenAI) validate against it server-side; the streaming endpoint emits
 * the JSON incrementally for the client to reassemble.
 *
 * <p>Every property is marked required and {@code additionalProperties} is
 * {@code false} so the schema satisfies OpenAI's strict-mode constraints.
 *
 * <p>The human-facing prose lives in {@code answer} as Markdown; the remaining
 * fields are machine-consumable metadata about the answer.
 */
public final class AgentResponseSchema {

    /** Schema name surfaced to the provider; also the JSON schema title. */
    public static final String NAME = "agent_response";

    private static final ResponseFormat RESPONSE_FORMAT = build();

    private AgentResponseSchema() {
    }

    /** The shared {@link ResponseFormat} to attach to every chat request. */
    public static ResponseFormat responseFormat() {
        return RESPONSE_FORMAT;
    }

    private static ResponseFormat build() {

        JsonObjectSchema root = JsonObjectSchema.builder()
                .description("A single structured answer from the billing agent.")
                .addStringProperty(
                        "answer",
                        "The full customer-facing reply, formatted as Markdown."
                )
                .addStringProperty(
                        "summary",
                        "One sentence capturing the answer."
                )
                .addProperty(
                        "key_points",
                        JsonArraySchema.builder()
                                .description("The most important takeaways, one per item.")
                                .items(JsonStringSchema.builder().build())
                                .build()
                )
                .addEnumProperty(
                        "confidence",
                        List.of("high", "medium", "low"),
                        "How well the supplied data supports the answer."
                )
                .addBooleanProperty(
                        "data_sufficient",
                        "True only if the supplied context was enough to answer."
                )
                .addProperty(
                        "follow_up_actions",
                        JsonArraySchema.builder()
                                .description("Concrete next steps for the customer; empty if none.")
                                .items(JsonStringSchema.builder().build())
                                .build()
                )
                // Strict structured output requires every property to be listed as required.
                .required(
                        "answer",
                        "summary",
                        "key_points",
                        "confidence",
                        "data_sufficient",
                        "follow_up_actions"
                )
                .additionalProperties(false)
                .build();

        JsonSchema schema = JsonSchema.builder()
                .name(NAME)
                .rootElement(root)
                .build();

        return ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(schema)
                .build();
    }
}
