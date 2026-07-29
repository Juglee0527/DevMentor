package com.devmentor.knowledge;

import java.util.List;

public record KnowledgeDocument(
        String id,
        String title,
        String skillCode,
        String conceptCode,
        List<String> keywords,
        String content,
        String sourceUrl,
        String version,
        boolean active,
        Scope scope
) {

    public KnowledgeDocument {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }

    public enum Scope {
        PUBLIC
    }
}
