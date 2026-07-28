package com.devmentor.user.dto;

import com.devmentor.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String nickname,
        int careerYears,
        String currentRole,
        String learningGoal,
        List<String> interestedSkillCodes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNickname(),
                user.getCareerYears(),
                user.getCurrentRole(),
                user.getLearningGoal(),
                user.getInterestedSkills().stream().map(skill -> skill.getCode()).toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
