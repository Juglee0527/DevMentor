package com.devmentor.user.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record UserRequest(
        @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
        @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
        String nickname,
        @Min(value = 0, message = "개발 경력은 0년 이상이어야 합니다.")
        @Max(value = 60, message = "개발 경력은 60년 이하여야 합니다.")
        int careerYears,
        @NotBlank(message = "현재 역할은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "현재 역할은 100자 이하여야 합니다.")
        String currentRole,
        @NotBlank(message = "학습 목표는 비어 있을 수 없습니다.")
        @Size(max = 200, message = "학습 목표는 200자 이하여야 합니다.")
        String learningGoal,
        @NotNull(message = "관심 기술 목록이 필요합니다.")
        Set<String> interestedSkillCodes
) {
}
