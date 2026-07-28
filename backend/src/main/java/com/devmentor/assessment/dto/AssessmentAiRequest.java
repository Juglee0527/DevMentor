package com.devmentor.assessment.dto;

public record AssessmentAiRequest(
        String skillCode,
        String conceptCode,
        String conceptName,
        String question,
        String userAnswer,
        int currentUnderstandingScore
) {
}
