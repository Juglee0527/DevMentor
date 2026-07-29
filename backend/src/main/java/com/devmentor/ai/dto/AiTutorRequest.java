package com.devmentor.ai.dto;

import java.util.List;

public record AiTutorRequest(
        UserContext user,
        String currentQuestion,
        List<ConversationMessage> recentMessages,
        List<ConceptContext> conceptStatuses,
        List<AvailableConcept> availableConcepts
) {

    public record UserContext(
            String nickname,
            int careerYears,
            String currentRole,
            String learningGoal,
            List<String> interestedSkillCodes
    ) {
    }

    public record ConversationMessage(
            String role,
            String content
    ) {
    }

    public record ConceptContext(
            String skillCode,
            String conceptCode,
            int understandingScore,
            String learningStatus
    ) {
    }

    public record AvailableConcept(
            String skillCode,
            String conceptCode,
            String name,
            String difficulty
    ) {
    }
}
