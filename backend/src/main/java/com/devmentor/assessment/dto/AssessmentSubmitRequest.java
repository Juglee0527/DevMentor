package com.devmentor.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssessmentSubmitRequest(
        @NotNull(message = "사용자 ID가 필요합니다.")
        Long userId,
        @NotNull(message = "AI 메시지 ID가 필요합니다.")
        Long chatMessageId,
        @NotBlank(message = "기술 코드가 필요합니다.")
        @Size(max = 50)
        String skillCode,
        @NotBlank(message = "개념 코드가 필요합니다.")
        @Size(max = 100)
        String conceptCode,
        @NotBlank(message = "답변은 비어 있을 수 없습니다.")
        @Size(max = 10000, message = "답변은 10000자 이하여야 합니다.")
        String userAnswer
) {
}
