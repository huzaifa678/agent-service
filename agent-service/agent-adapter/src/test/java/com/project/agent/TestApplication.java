package com.project.agent;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

// ObjectMapper is provided by the main adapter.config.JacksonConfig (scanned here),
// so no test-only bean is needed.
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.project.agent")
public class TestApplication {
}
