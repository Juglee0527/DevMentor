package com.devmentor.feedback.controller;

import com.devmentor.common.response.ApiResponse;
import com.devmentor.feedback.dto.AiFeedbackRequest;
import com.devmentor.feedback.dto.AiFeedbackResponse;
import com.devmentor.feedback.dto.TrainingEligibilityResponse;
import com.devmentor.feedback.service.AiFeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/ai-feedback")
public class AiFeedbackController {

    private final AiFeedbackService feedbackService;

    public AiFeedbackController(AiFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ApiResponse<AiFeedbackResponse> submit(
            @Valid @RequestBody AiFeedbackRequest request
    ) {
        return ApiResponse.success("AI 답변 피드백을 저장했습니다.", feedbackService.submit(request));
    }

    @DeleteMapping("/{feedbackId}")
    public ApiResponse<Void> revoke(
            @PathVariable @Positive Long feedbackId,
            @RequestParam @Positive Long userId
    ) {
        feedbackService.revoke(feedbackId, userId);
        return ApiResponse.success("AI 답변 피드백과 학습 동의를 철회했습니다.", null);
    }

    @GetMapping("/training-eligibility")
    public ApiResponse<TrainingEligibilityResponse> getTrainingEligibility() {
        return ApiResponse.success(
                "LoRA 학습 적격성을 확인했습니다.",
                feedbackService.getTrainingEligibility()
        );
    }
}
