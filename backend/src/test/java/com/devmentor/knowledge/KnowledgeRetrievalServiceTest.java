package com.devmentor.knowledge;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeRetrievalServiceTest {

    @Test
    void retrievesRelevantActivePublicDocument() {
        KnowledgeDocument relation = document(
                "JPA-HIBERNATE-001",
                "JPA",
                "JPA_HIBERNATE_RELATION",
                List.of("JPA", "Hibernate", "같은 제품"),
                true
        );
        KnowledgeDocument unrelated = document(
                "GIT-BRANCH-001",
                "GIT",
                "BRANCH",
                List.of("Git", "branch"),
                true
        );
        KnowledgeRetrievalService service = new KnowledgeRetrievalService(
                List.of(unrelated, relation),
                true,
                3
        );

        List<KnowledgeDocument> result = service.search(
                "JPA와 Hibernate는 같은 제품인가요?"
        );

        assertThat(result).extracting(KnowledgeDocument::id)
                .containsExactly("JPA-HIBERNATE-001");
    }

    @Test
    void excludesInactiveAndIrrelevantDocuments() {
        KnowledgeDocument inactive = document(
                "OLD-GIT-BRANCH",
                "GIT",
                "BRANCH",
                List.of("Git", "branch"),
                false
        );
        KnowledgeRetrievalService service = new KnowledgeRetrievalService(
                List.of(inactive),
                true,
                3
        );

        assertThat(service.search("Git branch가 무엇인가요?")).isEmpty();
        assertThat(service.search("오늘 점심 메뉴를 추천해 주세요.")).isEmpty();
    }

    @Test
    void returnsNothingWhenRagIsDisabled() {
        KnowledgeRetrievalService service = new KnowledgeRetrievalService(
                List.of(document(
                        "GIT-BRANCH-001",
                        "GIT",
                        "BRANCH",
                        List.of("Git", "branch"),
                        true
                )),
                false,
                3
        );

        assertThat(service.search("Git branch가 무엇인가요?")).isEmpty();
    }

    @Test
    void rejectsConflictingActiveDocumentsForSameConcept() {
        KnowledgeDocument first = document(
                "GIT-BRANCH-001",
                "GIT",
                "BRANCH",
                List.of("Git"),
                true
        );
        KnowledgeDocument conflicting = document(
                "GIT-BRANCH-002",
                "GIT",
                "BRANCH",
                List.of("Git"),
                true
        );

        assertThatThrownBy(() -> KnowledgeCatalog.validate(List.of(first, conflicting)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("활성 검수 문서가 둘 이상");
    }

    private KnowledgeDocument document(
            String id,
            String skillCode,
            String conceptCode,
            List<String> keywords,
            boolean active
    ) {
        return new KnowledgeDocument(
                id,
                id,
                skillCode,
                conceptCode,
                keywords,
                "검수된 기술 설명입니다.",
                "https://example.com/" + id,
                "1",
                active,
                KnowledgeDocument.Scope.PUBLIC
        );
    }
}
