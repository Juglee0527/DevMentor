package com.devmentor.assessment.dto;

import com.devmentor.learning.entity.LearningStatus;

public record ReviewTargetResponse(
        Long chatMessageId,
        String skillCode,
        String skillName,
        String conceptCode,
        String conceptName,
        String question,
        int understandingScore,
        LearningStatus learningStatus
) {
}
