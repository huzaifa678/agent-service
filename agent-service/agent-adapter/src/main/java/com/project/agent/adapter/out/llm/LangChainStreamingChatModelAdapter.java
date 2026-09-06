package com.project.agent.adapter.out.llm;

import com.project.agent.adapter.out.llm.mapper.LangChainExceptionMapper;
import com.project.agent.adapter.out.llm.mapper.LangChainMapper;
import com.project.agent.adapter.out.llm.schema.AgentResponseSchema;
import com.project.agent.application.execution.port.out.llm.model.ChatResult;
import com.project.agent.application.execution.port.out.llm.port.StreamingChatModelPort;
import com.project.agent.application.execution.port.out.llm.streaming.StreamingChatHandler;
import com.project.agent.application.execution.port.out.tool.ToolCall;
import com.project.agent.application.execution.service.workflow.AgentExecutionContext;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * langchain4j implementation of {@link StreamingChatModelPort}. Streams the model
 * response through the provider's async callback and adapts each event onto the
 * application's {@link StreamingChatHandler}: partial tokens, any tool calls
 * surfaced on completion, the terminal {@link ChatResult}, and errors.
 *
 * <p>Marked {@link Primary} so it is the {@link StreamingChatModelPort} injected into
 * the {@code StreamingLlmInvocationService} decorator (which also implements the port).
 *
 * <p>Every request carries the shared {@link AgentResponseSchema}, so the streamed
 * tokens form a JSON object validated against the agent-response schema.
 *
 * <p>Single-provider: unlike the synchronous adapter there is no failover, because
 * tokens already delivered to the client cannot be replayed on a second provider.
 */
@Component
@Primary
public class LangChainStreamingChatModelAdapter implements StreamingChatModelPort {

    private final StreamingChatModel streamingModel;
    private final LangChainMapper mapper;
    private final LangChainExceptionMapper exceptionMapper;

    public LangChainStreamingChatModelAdapter(
            @Qualifier("primaryStreamingChatModel") StreamingChatModel streamingModel,
            LangChainMapper mapper,
            LangChainExceptionMapper exceptionMapper
    ) {
        this.streamingModel = streamingModel;
        this.mapper = mapper;
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public void stream(
            AgentExecutionContext context,
            StreamingChatHandler handler
    ) {

        dev.langchain4j.model.chat.request.ChatRequest chatRequest =
                dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(mapper.toLangChainMessages(context.prompt()))
                        .responseFormat(AgentResponseSchema.responseFormat())
                        .build();

        streamingModel.chat(
                chatRequest,
                new StreamingChatResponseHandler() {

                    @Override
                    public void onPartialResponse(String token) {
                        handler.onToken(token);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {

                        ChatResult result = mapper.toChatResult(response);

                        for (ToolCall toolCall : result.toolCalls()) {
                            handler.onToolCall(toolCall);
                        }

                        handler.onComplete(result);
                    }

                    @Override
                    public void onError(Throwable error) {
                        handler.onError(translate(context, error));
                    }
                }
        );
    }

    /**
     * Translates a provider streaming failure into the domain exception hierarchy,
     * mirroring the synchronous adapter so callers see the same error types on both
     * paths. Non-{@link RuntimeException} throwables are passed through unchanged.
     */
    private Throwable translate(
            AgentExecutionContext context,
            Throwable error
    ) {

        if (error instanceof RuntimeException runtimeException) {

            return exceptionMapper.translate(
                    new com.project.agent.application.execution.port.out.llm.model.ChatRequest(
                            context.model(),
                            context.provider(),
                            context.prompt(),
                            context.enabledTools()
                    ),
                    "primary",
                    runtimeException
            );
        }

        return error;
    }
}
