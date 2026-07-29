package com.devmentor.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class KnowledgeCatalog {

    private static final String CATALOG_PATH = "knowledge/catalog.json";

    private final List<KnowledgeDocument> documents;

    public KnowledgeCatalog(ObjectMapper objectMapper) {
        this.documents = load(objectMapper);
        validate(documents);
    }

    public List<KnowledgeDocument> documents() {
        return documents;
    }

    private List<KnowledgeDocument> load(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            return List.copyOf(objectMapper.readValue(
                    input,
                    new TypeReference<List<KnowledgeDocument>>() {
                    }
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("검수 지식 카탈로그를 읽지 못했습니다.", exception);
        }
    }

    static void validate(List<KnowledgeDocument> documents) {
        Set<String> ids = new HashSet<>();
        Set<String> activeConcepts = new HashSet<>();

        for (KnowledgeDocument document : documents) {
            if (isBlank(document.id()) || isBlank(document.title())
                    || isBlank(document.content()) || isBlank(document.version())
                    || document.scope() == null) {
                throw new IllegalStateException("검수 지식 문서의 필수 값이 비어 있습니다.");
            }
            if (!ids.add(document.id())) {
                throw new IllegalStateException("중복된 검수 지식 문서 ID가 있습니다: " + document.id());
            }
            if (document.active()) {
                String conceptKey = document.skillCode() + ":" + document.conceptCode();
                if (!activeConcepts.add(conceptKey)) {
                    throw new IllegalStateException(
                            "같은 개념에 활성 검수 문서가 둘 이상입니다: " + conceptKey
                    );
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
