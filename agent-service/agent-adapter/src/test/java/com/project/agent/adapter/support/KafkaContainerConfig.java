package com.project.agent.adapter.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

// Extends the Postgres config so a @SpringBootTest using Kafka also gets a datasource
public abstract class KafkaContainerConfig extends PostgreSQLContainerConfig {

    static final KafkaContainer kafka =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka:3.8.0"
                    )
            );

    static {
        kafka.start();
    }

    protected static KafkaContainer kafkaContainer() {
        return kafka;
    }

    @DynamicPropertySource
    static void kafkaProperties(
            DynamicPropertyRegistry registry
    ) {

        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }
}
