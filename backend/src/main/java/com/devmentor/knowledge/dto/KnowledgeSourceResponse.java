package com.devmentor.knowledge.dto;

import com.devmentor.knowledge.KnowledgeDocument;

public record KnowledgeSourceResponse(
        String id,
        String title,
        String sourceUrl,
        String version
) {

    public static KnowledgeSourceResponse from(KnowledgeDocument document) {
        return new KnowledgeSourceResponse(
                document.id(),
                document.title(),
                document.sourceUrl(),
                document.version()
        );
    }
}
