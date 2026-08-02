package com.project.agent.adapter.in.rest.conversation;

import com.project.agent.adapter.in.rest.dto.AddMessageRequest;
import com.project.agent.adapter.in.rest.dto.RenameConversationRequest;
import com.project.agent.adapter.in.rest.dto.StartConversationRequest;
import com.project.agent.adapter.support.PostgreSQLContainerConfig;
import com.project.agent.adapter.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Testcontainers
class ConversationControllerIT extends PostgreSQLContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID fixedTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UUID fixedUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void start_createsConversation_returns201() throws Exception {
        StartConversationRequest request = TestDataFactory.startConversationRequest(
                fixedTenantId, fixedUserId, "Integration Test Conversation"
        );

        mockMvc.perform(post("/api/agent/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(fixedTenantId.toString()))
                .andExpect(jsonPath("$.userId").value(fixedUserId.toString()))
                .andExpect(jsonPath("$.title").value("Integration Test Conversation"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.messageCount").value(0))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getById_existingConversation_returns200() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Find Me");

        mockMvc.perform(get("/api/agent/conversations/{id}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.title").value("Find Me"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getById_nonExistingConversation_returns404() throws Exception {
        mockMvc.perform(get("/api/agent/conversations/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ConversationNotFoundException"));
    }

    @Test
    void byUser_returnsConversationsForUser() throws Exception {
        createConversation(fixedTenantId, fixedUserId, "User Conv 1");
        createConversation(fixedTenantId, fixedUserId, "User Conv 2");

        mockMvc.perform(get("/api/agent/conversations")
                        .param("userId", fixedUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(fixedUserId.toString()))
                .andExpect(jsonPath("$[1].userId").value(fixedUserId.toString()));
    }

    @Test
    void addMessage_toActiveConversation_returnsUpdatedConversation() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Msg Test");

        AddMessageRequest messageRequest = new AddMessageRequest("USER", "Hello there", 10, 0);

        mockMvc.perform(post("/api/agent/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageCount").value(1))
                .andExpect(jsonPath("$.id").value(conversationId));
    }

    @Test
    void addMessage_toArchivedConversation_returns422() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Archive Test");

        mockMvc.perform(post("/api/agent/conversations/{id}/archive", conversationId))
                .andExpect(status().isNoContent());

        AddMessageRequest messageRequest = new AddMessageRequest("USER", "Should fail", 5, 0);

        mockMvc.perform(post("/api/agent/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("InvalidConversationStateException"));
    }

    @Test
    void rename_existingConversation_returns204() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Old Title");

        RenameConversationRequest renameRequest = new RenameConversationRequest("New Title");

        mockMvc.perform(patch("/api/agent/conversations/{id}", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/agent/conversations/{id}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void archive_activeConversation_returns204() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Archive Me");

        mockMvc.perform(post("/api/agent/conversations/{id}/archive", conversationId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/agent/conversations/{id}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void delete_existingConversation_returns204() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Delete Me");

        mockMvc.perform(delete("/api/agent/conversations/{id}", conversationId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/agent/conversations/{id}", conversationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void messages_existingConversation_returnsMessageList() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Messages Test");

        mockMvc.perform(post("/api/agent/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMessageRequest("USER", "First", 10, 0))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/agent/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMessageRequest("ASSISTANT", "Second", 20, 0))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/agent/conversations/{id}/messages", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value("First"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$[1].content").value("Second"));
    }

    private String createConversation(UUID tenantId, UUID userId, String title) throws Exception {
        StartConversationRequest request = TestDataFactory.startConversationRequest(tenantId, userId, title);

        String response = mockMvc.perform(post("/api/agent/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
