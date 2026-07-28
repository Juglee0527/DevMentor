package com.devmentor.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiTutorResponse(
        @NotBlank @Size(max = 20000) String answer,
        @NotNull @Size(max = 20) List<@Valid DetectedConcept> detectedConcepts,
        @NotNull @Size(max = 20) List<@Valid KnowledgeGap> knowledgeGaps,
        @Size(max = 1000) String followUpQuestion,
        @NotNull @Size(max = 20) List<@Valid RecommendedConcept> recommendedConcepts
) {

    private static final int MAX_FALLBACK_LENGTH = 20000;

    public AiTutorResponse {
        detectedConcepts = detectedConcepts == null ? null : List.copyOf(detectedConcepts);
        knowledgeGaps = knowledgeGaps == null ? null : List.copyOf(knowledgeGaps);
        recommendedConcepts = recommendedConcepts == null ? null : List.copyOf(recommendedConcepts);
    }

    public static AiTutorResponse fallback(String rawText) {
        String answer = rawText == null ? "" : rawText.trim();
        if (answer.length() > MAX_FALLBACK_LENGTH) {
            answer = answer.substring(0, MAX_FALLBACK_LENGTH);
        }
        return new AiTutorResponse(answer, List.of(), List.of(), null, List.of());
    }

    public record DetectedConcept(
            @NotBlank @Size(max = 50) String skillCode,
            @NotBlank @Size(max = 100) String conceptCode,
            @DecimalMin("0.0") @DecimalMax("1.0") double confidence
    ) {
    }

    public record KnowledgeGap(
            @NotBlank @Size(max = 50) String skillCode,
            @NotBlank @Size(max = 100) String conceptCode,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }

    public record RecommendedConcept(
            @NotBlank @Size(max = 50) String skillCode,
            @NotBlank @Size(max = 100) String conceptCode,
            @NotBlank @Size(max = 1000) String reason
    ) {
    }
}
