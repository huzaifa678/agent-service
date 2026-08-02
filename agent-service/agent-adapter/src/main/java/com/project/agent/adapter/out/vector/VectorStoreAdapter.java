package com.project.agent.adapter.out.vector;

import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.MessageId;

import java.util.List;

/**
 * No-op {@link com.project.agent.application.execution.port.out.rag.VectorStorePort} stub.
 * The production, pgvector-backed implementation is {@link PgVectorStoreAdapter}.
 */
public class VectorStoreAdapter implements VectorStorePort {
    @Override
    public void index(ConversationId conversationId, MessageId messageId, String content) {

    }

    @Override
    public List<RetrievedPassage> search(ConversationId conversationId, String query, int topK) {
        return List.of();
    }
}
