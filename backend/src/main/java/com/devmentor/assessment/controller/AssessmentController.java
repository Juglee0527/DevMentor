package com.devmentor.assessment.controller;

import com.devmentor.assessment.dto.AssessmentResponse;
import com.devmentor.assessment.dto.AssessmentSubmitRequest;
import com.devmentor.assessment.dto.ReviewTargetResponse;
import com.devmentor.assessment.service.AssessmentService;
import com.devmentor.assessment.service.ReviewQueryService;
import com.devmentor.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final ReviewQueryService reviewQueryService;

    public AssessmentController(
            AssessmentService assessmentService,
            ReviewQueryService reviewQueryService
    ) {
        this.assessmentService = assessmentService;
        this.reviewQueryService = reviewQueryService;
    }

    @PostMapping("/assessments")
    public ApiResponse<AssessmentResponse> submit(
            @Valid @RequestBody AssessmentSubmitRequest request
    ) {
        return ApiResponse.success(
                "답변 평가를 완료했습니다.",
                assessmentService.submit(request)
        );
    }

    @GetMapping("/assessments")
    public ApiResponse<List<AssessmentResponse>> getAssessments(
            @RequestParam Long userId
    ) {
        return ApiResponse.success(
                "평가 이력을 조회했습니다.",
                assessmentService.getAssessments(userId)
        );
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ReviewTargetResponse>> getReviews(
            @RequestParam Long userId
    ) {
        return ApiResponse.success(
                "복습 대상을 조회했습니다.",
                reviewQueryService.getReviewTargets(userId)
        );
    }
}
