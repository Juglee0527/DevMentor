package com.devmentor.feedback.dto;

import com.devmentor.feedback.entity.FeedbackRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AiFeedbackRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long chatMessageId,
        @NotNull FeedbackRating rating,
        @Size(max = 20000) String correctedAnswer,
        boolean trainingConsent
) {
}
