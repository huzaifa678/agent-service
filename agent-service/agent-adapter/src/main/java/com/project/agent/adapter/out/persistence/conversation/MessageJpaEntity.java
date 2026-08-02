package com.project.agent.adapter.out.persistence.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** JPA persistence model for a {@code Message} (child of the conversation aggregate). */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageJpaEntity {

    @Id
    private UUID id;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(length = 16, nullable = false)
    private String role;

    private int promptTokens;

    private int completionTokens;

    @Column(nullable = false)
    private Instant createdAt;
}
