package com.project.agent.adapter.out.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Points JPA at the persistence models and Spring Data repositories, which live
 * under {@code com.project.agent.adapter.out.persistence} rather than under the
 * bootstrap package (so Boot's package-based auto-detection would miss them).
 */
@Configuration
@EntityScan(basePackages = "com.project.agent.adapter.out.persistence")
@EnableJpaRepositories(basePackages = "com.project.agent.adapter.out.persistence")
public class PersistenceConfig {
}
