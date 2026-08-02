package com.project.agent.application.execution.port.out.rag;

/** A passage returned from the vector store during RAG retrieval, with its similarity score. */
public record RetrievedPassage(
        String content,
        double score
) {}
