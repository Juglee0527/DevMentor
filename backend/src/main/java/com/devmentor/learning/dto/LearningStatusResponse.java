package com.devmentor.learning.dto;

import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.skill.entity.ConceptDifficulty;

import java.time.LocalDateTime;
import java.util.List;

public record LearningStatusResponse(
        List<SkillLearningStatus> skills
) {

    public record SkillLearningStatus(
            String skillCode,
            String skillName,
            int averageScore,
            List<ConceptLearningStatus> concepts
    ) {
    }

    public record ConceptLearningStatus(
            String conceptCode,
            String conceptName,
            ConceptDifficulty difficulty,
            int understandingScore,
            LearningStatus learningStatus,
            String assessmentReason,
            LocalDateTime lastStudiedAt,
            LocalDateTime nextReviewAt
    ) {
    }
}
