package com.devmentor.assessment.service;

import com.devmentor.assessment.dto.AssessmentAiRequest;

public record AssessmentPreparation(
        Long userId,
        Long chatMessageId,
        String skillCode,
        String conceptCode,
        String conceptName,
        String question,
        String userAnswer,
        int currentUnderstandingScore
) {

    public AssessmentAiRequest toAiRequest() {
        return new AssessmentAiRequest(
                skillCode,
                conceptCode,
                conceptName,
                question,
                userAnswer,
                currentUnderstandingScore
        );
    }
}
