package com.devmentor.learning.controller;

import com.devmentor.common.response.ApiResponse;
import com.devmentor.learning.dto.LearningRecommendationResponse;
import com.devmentor.learning.service.LearningRecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningRecommendationService recommendationService;

    public LearningController(LearningRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<LearningRecommendationResponse>> getRecommendations(
            @RequestParam Long userId
    ) {
        return ApiResponse.success(
                "추천 학습 개념을 조회했습니다.",
                recommendationService.getRecommendations(userId)
        );
    }
}
