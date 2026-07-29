package com.devmentor.feedback.dto;

import com.devmentor.feedback.entity.AiFeedback;
import com.devmentor.feedback.entity.FeedbackRating;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record AiFeedbackResponse(
        Long id,
        Long chatMessageId,
        FeedbackRating rating,
        String correctedAnswer,
        boolean trainingConsent,
        LocalDateTime consentedAt,
        String modelProvider,
        String modelName,
        String modelVersion,
        String promptVersion,
        Long responseTimeMs,
        String failureType,
        List<String> sourceIds,
        LocalDateTime updatedAt
) {

    public static AiFeedbackResponse from(AiFeedback feedback) {
        return new AiFeedbackResponse(
                feedback.getId(),
                feedback.getChatMessage().getId(),
                feedback.getRating(),
                feedback.getCorrectedAnswer(),
                feedback.isTrainingConsent(),
                feedback.getConsentedAt(),
                feedback.getModelProvider(),
                feedback.getModelName(),
                feedback.getModelVersion(),
                feedback.getPromptVersion(),
                feedback.getResponseTimeMs(),
                feedback.getFailureType(),
                parseSourceIds(feedback.getSourceIds()),
                feedback.getUpdatedAt()
        );
    }

    private static List<String> parseSourceIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(sourceId -> !sourceId.isBlank())
                .toList();
    }
}
