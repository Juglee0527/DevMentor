package com.devmentor.dashboard.dto;

import com.devmentor.chat.dto.ChatRoomResponse;
import com.devmentor.learning.entity.LearningStatus;

import java.util.List;

public record DashboardResponse(
        UserSummary user,
        int overallUnderstandingScore,
        int totalConceptCount,
        int startedConceptCount,
        int reviewTargetCount,
        List<SkillProgress> skillProgress,
        List<WeakConcept> weakConcepts,
        List<ChatRoomResponse> recentChats
) {

    public record UserSummary(
            Long id,
            String nickname,
            int careerYears,
            String currentRole,
            String learningGoal
    ) {
    }

    public record SkillProgress(
            String skillCode,
            String skillName,
            int averageScore,
            int startedConceptCount,
            int totalConceptCount
    ) {
    }

    public record WeakConcept(
            String skillCode,
            String skillName,
            String conceptCode,
            String conceptName,
            int understandingScore,
            LearningStatus learningStatus,
            String reason
    ) {
    }
}
