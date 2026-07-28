package com.devmentor.learning.service;

import com.devmentor.learning.dto.LearningRecommendationResponse;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.entity.UserConceptStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LearningRecommendationService {

    private final UserService userService;
    private final UserConceptStatusRepository statusRepository;

    public LearningRecommendationService(
            UserService userService,
            UserConceptStatusRepository statusRepository
    ) {
        this.userService = userService;
        this.statusRepository = statusRepository;
    }

    public List<LearningRecommendationResponse> getRecommendations(Long userId) {
        userService.findUser(userId);
        return statusRepository.findAllWithConceptByUserId(userId).stream()
                .filter(status -> status.getUnderstandingScore() < 80)
                .sorted(Comparator.<UserConceptStatus>comparingInt(
                                status -> statusPriority(status.getLearningStatus()))
                        .thenComparingInt(status -> status.getUnderstandingScore())
                        .thenComparingInt(status -> status.getConcept().getSkill().getDisplayOrder())
                        .thenComparingInt(status -> status.getConcept().getDisplayOrder()))
                .map(LearningRecommendationResponse::from)
                .toList();
    }

    private int statusPriority(LearningStatus status) {
        return switch (status) {
            case NEEDS_REVIEW -> 0;
            case LEARNING -> 1;
            case NOT_STARTED -> 2;
            case UNDERSTOOD -> 3;
        };
    }
}
