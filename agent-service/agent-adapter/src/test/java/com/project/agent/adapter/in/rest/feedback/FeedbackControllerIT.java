package com.project.agent.adapter.in.rest.feedback;

import com.project.agent.adapter.in.rest.dto.SubmitFeedbackRequest;
import com.project.agent.adapter.in.rest.dto.UpdateFeedbackRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.agent.adapter.support.PostgreSQLContainerConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Testcontainers
class FeedbackControllerIT extends PostgreSQLContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID fixedTenantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UUID fixedUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void submit_createsFeedback_returns201() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Feedback Test");
        UUID messageId = addMessage(conversationId, "USER", "Rate me", 5, 0);

        SubmitFeedbackRequest request = new SubmitFeedbackRequest(
                UUID.fromString(conversationId), messageId, 5, "Great response"
        );

        mockMvc.perform(post("/api/agent/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value(conversationId))
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.positive").value(true))
                .andExpect(jsonPath("$.comment").value("Great response"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getById_existingFeedback_returns200() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Get Feedback");
        UUID messageId = addMessage(conversationId, "USER", "Test", 5, 0);
        String feedbackId = submitFeedback(conversationId, messageId, 4, "Good");

        mockMvc.perform(get("/api/agent/feedback/{id}", feedbackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(feedbackId))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.positive").value(true))
                .andExpect(jsonPath("$.comment").value("Good"));
    }

    @Test
    void getById_nonExistingFeedback_returns404() throws Exception {
        mockMvc.perform(get("/api/agent/feedback/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FeedbackNotFoundException"));
    }

    @Test
    void byConversation_returnsFeedbackList() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Multi Feedback");
        UUID messageId = addMessage(conversationId, "USER", "Test", 5, 0);

        submitFeedback(conversationId, messageId, 5, "Excellent");
        submitFeedback(conversationId, messageId, 2, "Poor");

        mockMvc.perform(get("/api/agent/feedback")
                        .param("conversationId", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[1].rating").value(2));
    }

    @Test
    void update_existingFeedback_returnsUpdatedFeedback() throws Exception {
        String conversationId = createConversation(fixedTenantId, fixedUserId, "Update Feedback");
        UUID messageId = addMessage(conversationId, "USER", "Test", 5, 0);
        String feedbackId = submitFeedback(conversationId, messageId, 3, "Average");

        UpdateFeedbackRequest updateRequest = new UpdateFeedbackRequest(5, "Actually great");

        mockMvc.perform(patch("/api/agent/feedback/{id}", feedbackId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.positive").value(true))
                .andExpect(jsonPath("$.comment").value("Actually great"));
    }

    @Test
    void update_nonExistingFeedback_returns404() throws Exception {
        UpdateFeedbackRequest updateRequest = new UpdateFeedbackRequest(5, "Great");

        mockMvc.perform(patch("/api/agent/feedback/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FeedbackNotFoundException"));
    }

    private String createConversation(UUID tenantId, UUID userId, String title) throws Exception {
        String response = mockMvc.perform(post("/api/agent/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.project.agent.adapter.in.rest.dto.StartConversationRequest(
                                        tenantId, userId, title)))
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    private UUID addMessage(String conversationId, String role, String content, int promptTokens, int completionTokens) throws Exception {
        String response = mockMvc.perform(post("/api/agent/conversations/{id}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.project.agent.adapter.in.rest.dto.AddMessageRequest(
                                        role, content, promptTokens, completionTokens))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private String submitFeedback(String conversationId, UUID messageId, int rating, String comment) throws Exception {
        SubmitFeedbackRequest request = new SubmitFeedbackRequest(
                UUID.fromString(conversationId), messageId, rating, comment
        );

        String response = mockMvc.perform(post("/api/agent/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
