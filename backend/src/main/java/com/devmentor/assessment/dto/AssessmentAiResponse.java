package com.devmentor.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssessmentAiResponse(
        boolean correct,
        @Min(0) @Max(100) int score,
        @NotBlank @Size(max = 5000) String feedback,
        @NotBlank @Size(max = 5000) String correctAnswer,
        boolean reviewRequired
) {
}
