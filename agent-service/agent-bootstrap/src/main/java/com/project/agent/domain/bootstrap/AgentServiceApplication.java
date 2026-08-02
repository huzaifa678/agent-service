package com.project.agent.domain.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entrypoint lives in {@code com.project.agent.domain.bootstrap}, which is
 * deeper than the components in {@code com.project.agent.adapter} /
 * {@code .application}. {@code scanBasePackages} widens the component scan to the
 * whole {@code com.project.agent} tree; JPA entity/repository scanning is set in
 * {@code com.project.agent.adapter.out.persistence.PersistenceConfig}.
 */
@SpringBootApplication(scanBasePackages = "com.project.agent")
public class AgentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentServiceApplication.class, args);
	}

}
