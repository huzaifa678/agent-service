package com.project.agent.application.execution.service.common;

import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.exception.ConversationDeletedException;
import com.project.agent.domain.conversation.exception.InvalidConversationStateException;
import org.springframework.stereotype.Service;

/**
 * Validates whether a conversation can participate in a new agent execution.
 *
 * <p>This service encapsulates conversation lifecycle rules required before
 * an agent turn begins. Only active conversations may accept additional
 * messages or agent executions.
 *
 * <p>Keeping these rules separate from the application service makes the
 * orchestration layer easier to read and allows conversation validation
 * to be tested independently.
 */
@Service
public class ConversationValidationService {

    /**
     * Ensures the supplied conversation can accept new messages.
     *
     * @param conversation conversation being executed against
     * @throws ConversationDeletedException if the conversation was deleted
     * @throws InvalidConversationStateException if the conversation is archived
     */
    public void validate(Conversation conversation) {

        switch (conversation.getStatus()) {

            case ACTIVE -> {
                // valid
            }

            case DELETED -> throw new ConversationDeletedException();

            case ARCHIVED -> throw new InvalidConversationStateException(
                    "Cannot run the agent on an archived conversation."
            );
        }
    }
}
