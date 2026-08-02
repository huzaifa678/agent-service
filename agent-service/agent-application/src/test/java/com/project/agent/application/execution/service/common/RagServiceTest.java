package com.project.agent.application.execution.service.common;

import com.project.agent.application.execution.port.out.llm.model.ChatMessage;
import com.project.agent.application.execution.port.out.rag.RetrievedPassage;
import com.project.agent.application.execution.port.out.rag.VectorStorePort;
import com.project.agent.application.execution.service.common.builder.MessageBuilder;
import com.project.agent.domain.execution.agent.exception.ConversationNotFoundInVectorStoreException;
import com.project.agent.domain.execution.agent.exception.EmbeddingGenerationException;
import com.project.agent.domain.execution.agent.exception.UnsupportedEmbeddingModelException;
import com.project.agent.domain.execution.agent.exception.VectorSearchException;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.identity.ConversationId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStorePort vectorStore;

    private RagService service;

    @BeforeEach
    void setUp() {
        service = new RagService(vectorStore);
    }

    @Test
    void retrieveContext_returnsSystemMessages() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(
                conversationId,
                "billing",
                5
        )).thenReturn(List.of(
                new RetrievedPassage("Passage one", 0.95),
                new RetrievedPassage("Passage two", 0.88)
        ));

        List<ChatMessage> result =
                service.retrieveContext(
                        conversationId,
                        "billing"
                );

        assertEquals(2, result.size());

        assertEquals(
                MessageRole.SYSTEM,
                result.get(0).role()
        );
        assertEquals(
                "Passage one",
                result.get(0).content()
        );

        assertEquals(
                MessageRole.SYSTEM,
                result.get(1).role()
        );
        assertEquals(
                "Passage two",
                result.get(1).content()
        );

        verify(vectorStore).search(
                conversationId,
                "billing",
                5
        );
    }

    @Test
    void retrieveContext_returnsEmptyList_whenConversationNotIndexed() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(
                any(),
                anyString(),
                anyInt()
        )).thenThrow(
                new ConversationNotFoundInVectorStoreException(conversationId.toString())
        );

        List<ChatMessage> result =
                service.retrieveContext(
                        conversationId,
                        "billing"
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void retrieveContext_returnsEmptyList_whenVectorSearchFails() {

        ConversationId conversationId = ConversationId.of(UUID.randomUUID());

        when(vectorStore.search(
                any(),
                anyString(),
                anyInt()
        )).thenThrow(
                new VectorSearchException("Search failed")
        );

        List<ChatMessage> result =
                service.retrieveContext(
                        conversationId,
                        "billing"
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void indexAssistantResponse_indexesMessage() {

        ConversationId conversationId =
                ConversationId.of(UUID.randomUUID());

        Message assistant =
                MessageBuilder.aMessage().withContent("Hello from assistant")
                        .build();

        assertDoesNotThrow(() ->
                service.indexAssistantResponse(
                        conversationId,
                        assistant
                )
        );

        verify(vectorStore).index(
                conversationId,
                assistant.getId(),
                "Hello from assistant"
        );
    }

    @Test
    void indexAssistantResponse_propagatesUnsupportedEmbeddingModelException() {

        ConversationId conversationId =
                ConversationId.of(UUID.randomUUID());

        Message assistant =
                MessageBuilder.aMessage().build();

        doThrow(new UnsupportedEmbeddingModelException("bge-large"))
                .when(vectorStore)
                .index(any(), any(), anyString());

        assertThrows(
                UnsupportedEmbeddingModelException.class,
                () -> service.indexAssistantResponse(
                        conversationId,
                        assistant
                )
        );
    }

    @Test
    void indexAssistantResponse_ignoresEmbeddingGenerationException() {

        ConversationId conversationId =
                ConversationId.of(UUID.randomUUID());

        Message assistant =
                MessageBuilder.aMessage().build();

        doThrow(new EmbeddingGenerationException("Embedding failed"))
                .when(vectorStore)
                .index(any(), any(), anyString());

        assertDoesNotThrow(() ->
                service.indexAssistantResponse(
                        conversationId,
                        assistant
                )
        );
    }
}
