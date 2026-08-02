package com.project.agent.application.conversation.port.in;

import com.project.agent.domain.conversation.Conversation;

/** Inbound port: append a message to a conversation, returning the updated aggregate. */
public interface AddMessageUseCase {

    Conversation addMessage(AddMessageCommand command);
}
