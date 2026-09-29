package com.project.agent.adapter.out.persistence.conversation;

import com.project.agent.adapter.out.persistence.conversation.ConversationPersistenceAdapter;
import com.project.agent.adapter.support.PostgreSQLContainerConfig;
import com.project.agent.domain.conversation.Conversation;
import com.project.agent.domain.conversation.ConversationStatus;
import com.project.agent.domain.message.Message;
import com.project.agent.domain.message.MessageRole;
import com.project.agent.domain.vo.ai.TokenUsage;
import com.project.agent.domain.vo.conversation.ConversationTitle;
import com.project.agent.domain.vo.conversation.MessageContent;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.TenantId;
import com.project.agent.domain.vo.identity.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Testcontainers
class ConversationPersistenceAdapterIT extends PostgreSQLContainerConfig {

    @Autowired
    private ConversationPersistenceAdapter adapter;

    @Autowired
    private ConversationJpaRepository repository;

    @Test
    void save_persistsConversationWithMessages() {
        Conversation conversation = new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Persistence Test")
        );

        conversation.addMessage(new Message(
                com.project.agent.domain.vo.identity.MessageId.of(UUID.randomUUID()),
                MessageContent.of("Hello"),
                MessageRole.USER,
                TokenUsage.of(10, 0)
        ));

        adapter.project(conversation);
        Conversation saved = adapter.findById(conversation.getId()).orElseThrow();

        assertThat(saved.getId()).isEqualTo(conversation.getId());
        assertThat(saved.getTitle()).isEqualTo(ConversationTitle.of("Persistence Test"));
        assertThat(saved.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(saved.getMessages().size()).isEqualTo(1);
        assertThat(saved.getMessages().getFirst().getContent()).isEqualTo(MessageContent.of("Hello"));
    }

    @Test
    void findById_existingConversation_returnsConversation() {
        Conversation conversation = new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Find Me")
        );
        adapter.project(conversation);

        Optional<Conversation> found = adapter.findById(conversation.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(conversation.getId());
        assertThat(found.get().getTitle()).isEqualTo(ConversationTitle.of("Find Me"));
    }

    @Test
    void findById_nonExistingConversation_returnsEmpty() {
        Optional<Conversation> found = adapter.findById(ConversationId.of(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId_returnsAllConversationsForUser() {
        UUID userId = UUID.randomUUID();

        Conversation conv1 = new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(userId),
                ConversationTitle.of("Conv 1")
        );
        Conversation conv2 = new Conversation(
                ConversationId.of(UUID.randomUUID()),
                TenantId.of(UUID.randomUUID()),
                UserId.of(userId),
                ConversationTitle.of("Conv 2")
        );

        adapter.project(conv1);
        adapter.project(conv2);

        List<Conversation> results = adapter.findByUserId(UserId.of(userId));

        assertThat(results).hasSize(2);
        assertThat(results).extracting("id")
                .containsExactlyInAnyOrder(conv1.getId(), conv2.getId());
    }

    @Test
    void save_thenFindById_roundTripsCorrectly() {
        UUID id = UUID.randomUUID();
        Conversation conversation = new Conversation(
                ConversationId.of(id),
                TenantId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                ConversationTitle.of("Round Trip")
        );
        conversation.addMessage(new Message(
                com.project.agent.domain.vo.identity.MessageId.of(UUID.randomUUID()),
                MessageContent.of("Message 1"),
                MessageRole.USER,
                TokenUsage.of(5, 0)
        ));

        adapter.project(conversation);

        ConversationJpaEntity jpaEntity = repository.findById(id).orElseThrow();
        assertThat(jpaEntity.getTitle()).isEqualTo("Round Trip");
        assertThat(jpaEntity.getMessages()).hasSize(1);
        assertThat(jpaEntity.getMessages().getFirst().getContent()).isEqualTo("Message 1");
    }
}
