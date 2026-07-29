package com.devmentor.ai.dto;

import java.util.List;

public record AiTutorRequest(
        UserContext user,
        String currentQuestion,
        List<ConversationMessage> recentMessages,
        List<ConceptContext> conceptStatuses,
        List<AvailableConcept> availableConcepts,
        List<RetrievedDocument> retrievedDocuments
) {

    public AiTutorRequest {
        recentMessages = List.copyOf(recentMessages);
        conceptStatuses = List.copyOf(conceptStatuses);
        availableConcepts = List.copyOf(availableConcepts);
        retrievedDocuments = List.copyOf(retrievedDocuments);
    }

    public AiTutorRequest(
            UserContext user,
            String currentQuestion,
            List<ConversationMessage> recentMessages,
            List<ConceptContext> conceptStatuses,
            List<AvailableConcept> availableConcepts
    ) {
        this(
                user,
                currentQuestion,
                recentMessages,
                conceptStatuses,
                availableConcepts,
                List.of()
        );
    }

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

    public record RetrievedDocument(
            String id,
            String title,
            String content,
            String sourceUrl,
            String version
    ) {
    }
}
