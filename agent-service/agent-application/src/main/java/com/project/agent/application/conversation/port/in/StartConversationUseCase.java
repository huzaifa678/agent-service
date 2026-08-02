package com.project.agent.application.conversation.port.in;

import com.project.agent.domain.conversation.Conversation;

/** Inbound port: open a new conversation. */
public interface StartConversationUseCase {

    Conversation start(StartConversationCommand command);
}
