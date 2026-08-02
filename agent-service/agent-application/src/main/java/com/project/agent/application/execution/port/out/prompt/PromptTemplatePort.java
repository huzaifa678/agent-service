package com.project.agent.application.execution.port.out.prompt;

/**
 * Outbound port for loading prompt templates by name from a template source
 * (e.g. the classpath).
 */
public interface PromptTemplatePort {

    String load(String templateName);
}
