package com.devmentor.learning.dto;

import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.entity.UserConceptStatus;

public record LearningRecommendationResponse(
        Long conceptStatusId,
        String skillCode,
        String skillName,
        String conceptCode,
        String conceptName,
        int understandingScore,
        LearningStatus learningStatus,
        String reason
) {

    public static LearningRecommendationResponse from(UserConceptStatus status) {
        return new LearningRecommendationResponse(
                status.getId(),
                status.getConcept().getSkill().getCode(),
                status.getConcept().getSkill().getName(),
                status.getConcept().getCode(),
                status.getConcept().getName(),
                status.getUnderstandingScore(),
                status.getLearningStatus(),
                status.getAssessmentReason()
        );
    }
}
