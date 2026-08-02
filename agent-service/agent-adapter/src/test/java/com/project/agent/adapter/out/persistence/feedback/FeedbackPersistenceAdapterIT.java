package com.project.agent.adapter.out.persistence.feedback;

import com.project.agent.adapter.out.persistence.feedback.FeedbackPersistenceAdapter;
import com.project.agent.adapter.support.PostgreSQLContainerConfig;
import com.project.agent.domain.feedback.Feedback;
import com.project.agent.domain.vo.billing.Rating;
import com.project.agent.domain.vo.identity.ConversationId;
import com.project.agent.domain.vo.identity.FeedbackId;
import com.project.agent.domain.vo.identity.MessageId;
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
class FeedbackPersistenceAdapterIT extends PostgreSQLContainerConfig {

    @Autowired
    private FeedbackPersistenceAdapter adapter;

    @Autowired
    private FeedbackJpaRepository repository;

    @Test
    void save_persistsFeedback() {
        Feedback feedback = Feedback.create(
                FeedbackId.of(UUID.randomUUID()),
                ConversationId.of(UUID.randomUUID()),
                MessageId.of(UUID.randomUUID()),
                Rating.of(5),
                "Excellent"
        );

        Feedback saved = adapter.save(feedback);

        assertThat(saved.getId()).isEqualTo(feedback.getId());
        assertThat(saved.getRating()).isEqualTo(Rating.of(5));
        assertThat(saved.getComment()).isEqualTo("Excellent");
        assertThat(saved.isPositive()).isTrue();
    }

    @Test
    void findById_existingFeedback_returnsFeedback() {
        Feedback feedback = Feedback.create(
                FeedbackId.of(UUID.randomUUID()),
                ConversationId.of(UUID.randomUUID()),
                MessageId.of(UUID.randomUUID()),
                Rating.of(4),
                "Good"
        );
        adapter.save(feedback);

        Optional<Feedback> found = adapter.findById(feedback.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(Rating.of(4));
        assertThat(found.get().getComment()).isEqualTo("Good");
    }

    @Test
    void findById_nonExistingFeedback_returnsEmpty() {
        Optional<Feedback> found = adapter.findById(FeedbackId.of(UUID.randomUUID()));
        assertThat(found).isEmpty();
    }

    @Test
    void findByConversationId_returnsFeedbackForConversation() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        Feedback feedback1 = Feedback.create(
                FeedbackId.of(UUID.randomUUID()),
                ConversationId.of(conversationId),
                MessageId.of(messageId),
                Rating.of(5),
                "Great"
        );
        Feedback feedback2 = Feedback.create(
                FeedbackId.of(UUID.randomUUID()),
                ConversationId.of(conversationId),
                MessageId.of(messageId),
                Rating.of(2),
                "Poor"
        );

        adapter.save(feedback1);
        adapter.save(feedback2);

        List<Feedback> results = adapter.findByConversationId(ConversationId.of(conversationId));

        assertThat(results).hasSize(2);
        assertThat(results).extracting("id")
                .containsExactlyInAnyOrder(feedback1.getId(), feedback2.getId());
    }

    @Test
    void save_thenFindById_roundTripsCorrectly() {
        UUID id = UUID.randomUUID();
        Feedback feedback = Feedback.create(
                FeedbackId.of(id),
                ConversationId.of(UUID.randomUUID()),
                MessageId.of(UUID.randomUUID()),
                Rating.of(3),
                "Average"
        );

        adapter.save(feedback);

        FeedbackJpaEntity jpaEntity = repository.findById(id).orElseThrow();
        assertThat(jpaEntity.getRating()).isEqualTo(3);
        assertThat(jpaEntity.getComment()).isEqualTo("Average");
        assertThat(jpaEntity.getConversationId()).isNotNull();
    }
}
