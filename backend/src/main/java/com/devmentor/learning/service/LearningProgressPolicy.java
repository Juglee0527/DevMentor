package com.devmentor.learning.service;

import com.devmentor.learning.entity.LearningStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LearningProgressPolicy {

    public ProgressUpdate apply(
            int currentScore,
            LearningEvidence evidence,
            String reason,
            LocalDateTime studiedAt
    ) {
        if (currentScore < 0 || currentScore > 100) {
            throw new IllegalArgumentException("현재 이해도 점수는 0부터 100 사이여야 합니다.");
        }
        int score = clamp(currentScore + scoreChange(evidence));
        LearningStatus status = resolveStatus(score, evidence);
        LocalDateTime nextReviewAt = studiedAt.plusDays(reviewIntervalDays(status));
        return new ProgressUpdate(score, status, reason, studiedAt, nextReviewAt);
    }

    private int scoreChange(LearningEvidence evidence) {
        return switch (evidence) {
            case DETECTED -> 10;
            case KNOWLEDGE_GAP -> -15;
            case CORRECT -> 20;
            case INCORRECT -> -20;
        };
    }

    private LearningStatus resolveStatus(int score, LearningEvidence evidence) {
        if (evidence == LearningEvidence.KNOWLEDGE_GAP
                || evidence == LearningEvidence.INCORRECT) {
            return LearningStatus.NEEDS_REVIEW;
        }
        if (score >= 80) {
            return LearningStatus.UNDERSTOOD;
        }
        return LearningStatus.LEARNING;
    }

    private long reviewIntervalDays(LearningStatus status) {
        return switch (status) {
            case NEEDS_REVIEW -> 1;
            case LEARNING -> 3;
            case UNDERSTOOD -> 7;
            case NOT_STARTED -> throw new IllegalStateException("학습 근거 적용 후 미시작 상태일 수 없습니다.");
        };
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    public record ProgressUpdate(
            int score,
            LearningStatus status,
            String reason,
            LocalDateTime lastStudiedAt,
            LocalDateTime nextReviewAt
    ) {
    }
}
