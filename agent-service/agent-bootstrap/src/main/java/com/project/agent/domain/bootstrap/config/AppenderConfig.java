package com.project.agent.domain.bootstrap.config;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Spring configuration that installs the OpenTelemetry Logback appender at startup so
 * application logs are exported over OTLP.
 */
@Configuration
public class AppenderConfig {

    @Bean
    public boolean OtelAppenderInstaller(OpenTelemetry openTelemetry) {
        OpenTelemetryAppender.install(openTelemetry);
        return true;
    }
}
