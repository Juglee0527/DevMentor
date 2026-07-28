package com.devmentor.learning.controller;

import com.devmentor.common.response.ApiResponse;
import com.devmentor.learning.dto.LearningRecommendationResponse;
import com.devmentor.learning.dto.LearningStatusResponse;
import com.devmentor.learning.service.LearningRecommendationService;
import com.devmentor.learning.service.LearningStatusQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningRecommendationService recommendationService;
    private final LearningStatusQueryService statusQueryService;

    public LearningController(
            LearningRecommendationService recommendationService,
            LearningStatusQueryService statusQueryService
    ) {
        this.recommendationService = recommendationService;
        this.statusQueryService = statusQueryService;
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

    @GetMapping("/status")
    public ApiResponse<LearningStatusResponse> getStatus(@RequestParam Long userId) {
        return ApiResponse.success(
                "학습 현황을 조회했습니다.",
                statusQueryService.getStatus(userId)
        );
    }
}
