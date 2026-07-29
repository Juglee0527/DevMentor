package com.devmentor.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class KnowledgeRetrievalService {

    private static final int MINIMUM_SCORE = 10;
    private static final Pattern TOKEN_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}+#.]+");

    private final List<KnowledgeDocument> documents;
    private final boolean enabled;
    private final int maxDocuments;

    public KnowledgeRetrievalService(
            KnowledgeCatalog catalog,
            @Value("${app.ai.rag.enabled:true}") boolean enabled,
            @Value("${app.ai.rag.max-documents:3}") int maxDocuments
    ) {
        this(catalog.documents(), enabled, maxDocuments);
    }

    KnowledgeRetrievalService(
            List<KnowledgeDocument> documents,
            boolean enabled,
            int maxDocuments
    ) {
        if (maxDocuments < 1 || maxDocuments > 5) {
            throw new IllegalArgumentException("RAG 최대 문서 수는 1~5여야 합니다.");
        }
        this.documents = List.copyOf(documents);
        this.enabled = enabled;
        this.maxDocuments = maxDocuments;
    }

    public List<KnowledgeDocument> search(String question) {
        if (!enabled || question == null || question.isBlank()) {
            return List.of();
        }

        String normalizedQuestion = normalize(question);
        Set<String> questionTokens = tokens(normalizedQuestion);

        return documents.stream()
                .filter(KnowledgeDocument::active)
                .filter(document -> document.scope() == KnowledgeDocument.Scope.PUBLIC)
                .map(document -> new ScoredDocument(document, score(
                        document,
                        normalizedQuestion,
                        questionTokens
                )))
                .filter(scored -> scored.score() >= MINIMUM_SCORE)
                .sorted(Comparator
                        .comparingInt(ScoredDocument::score).reversed()
                        .thenComparing(scored -> scored.document().id()))
                .limit(maxDocuments)
                .map(ScoredDocument::document)
                .toList();
    }

    private int score(
            KnowledgeDocument document,
            String normalizedQuestion,
            Set<String> questionTokens
    ) {
        int score = 0;
        for (String keyword : document.keywords()) {
            String normalizedKeyword = normalize(keyword);
            if (matchesKeyword(normalizedKeyword, normalizedQuestion, questionTokens)) {
                score += 10;
            }
        }

        Set<String> documentTokens = tokens(
                normalize(document.title() + " " + document.content())
        );
        for (String token : questionTokens) {
            if (token.length() >= 2 && documentTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private boolean matchesKeyword(
            String keyword,
            String normalizedQuestion,
            Set<String> questionTokens
    ) {
        if (keyword.isBlank()) {
            return false;
        }
        if (keyword.length() <= 4 && keyword.chars().allMatch(character ->
                (character >= 'a' && character <= 'z')
                        || Character.isDigit(character)
                        || character == '+'
                        || character == '#'
        )) {
            return questionTokens.contains(keyword);
        }
        return normalizedQuestion.contains(keyword);
    }

    private Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        for (String token : TOKEN_SEPARATOR.split(text)) {
            if (token.length() >= 2) {
                result.add(token);
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private record ScoredDocument(KnowledgeDocument document, int score) {
    }
}
