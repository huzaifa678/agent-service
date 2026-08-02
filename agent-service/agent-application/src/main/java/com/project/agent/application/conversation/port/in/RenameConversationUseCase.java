package com.project.agent.application.conversation.port.in;

/** Inbound port: rename an existing conversation. */
public interface RenameConversationUseCase {

    void rename(RenameConversationCommand command);
}
