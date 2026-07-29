package com.devmentor.feedback.dto;

import java.util.List;

public record TrainingEligibilityResponse(
        boolean eligible,
        long consentedFeedbackCount,
        long correctedAnswerCount,
        int minimumConsentedFeedback,
        int minimumCorrectedAnswers,
        boolean separateEvaluationDatasetReady,
        List<String> blockers
) {
}
