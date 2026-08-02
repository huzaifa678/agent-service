package com.project.agent.adapter.support;

import com.project.agent.adapter.in.rest.dto.RunAgentRequest;
import com.project.agent.adapter.in.rest.dto.StartConversationRequest;

import java.util.List;
import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static StartConversationRequest
    startConversationRequest() {

        return new StartConversationRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Conversation"
        );
    }

    public static RunAgentRequest
    runAgentRequest() {

        return new RunAgentRequest(
                UUID.randomUUID(),
                "Hello",
                "gpt-4o",
                "openai",
                List.of()
        );
    }

    public static StartConversationRequest
    startConversationRequest(
            UUID tenantId,
            UUID userId,
            String title
    ) {

        return new StartConversationRequest(tenantId, userId, title);
    }
}
