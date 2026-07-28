package com.devmentor.assessment.dto;

import com.devmentor.assessment.entity.Assessment;

import java.time.LocalDateTime;

public record AssessmentResponse(
        Long id,
        Long chatMessageId,
        String skillCode,
        String conceptCode,
        String conceptName,
        String question,
        String userAnswer,
        int score,
        boolean correct,
        String feedback,
        String correctAnswer,
        boolean reviewRequired,
        LocalDateTime createdAt
) {

    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getChatMessage().getId(),
                assessment.getConcept().getSkill().getCode(),
                assessment.getConcept().getCode(),
                assessment.getConcept().getName(),
                assessment.getQuestion(),
                assessment.getUserAnswer(),
                assessment.getScore(),
                assessment.isCorrect(),
                assessment.getFeedback(),
                assessment.getCorrectAnswer(),
                assessment.isReviewRequired(),
                assessment.getCreatedAt()
        );
    }
}
