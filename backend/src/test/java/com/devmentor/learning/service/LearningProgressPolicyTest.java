package com.devmentor.learning.service;

import com.devmentor.learning.entity.LearningStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningProgressPolicyTest {

    private final LearningProgressPolicy policy = new LearningProgressPolicy();
    private final LocalDateTime studiedAt = LocalDateTime.of(2026, 7, 28, 10, 0);

    @Test
    void detectedConceptStartsLearningAndCapsScoreAtOneHundred() {
        var first = policy.apply(0, LearningEvidence.DETECTED, "감지", studiedAt);
        var capped = policy.apply(95, LearningEvidence.DETECTED, "감지", studiedAt);

        assertThat(first.score()).isEqualTo(10);
        assertThat(first.status()).isEqualTo(LearningStatus.LEARNING);
        assertThat(first.nextReviewAt()).isEqualTo(studiedAt.plusDays(3));
        assertThat(capped.score()).isEqualTo(100);
        assertThat(capped.status()).isEqualTo(LearningStatus.UNDERSTOOD);
    }

    @Test
    void knowledgeGapHasPriorityAndDoesNotDropBelowZero() {
        var update = policy.apply(0, LearningEvidence.KNOWLEDGE_GAP, "공백", studiedAt);

        assertThat(update.score()).isZero();
        assertThat(update.status()).isEqualTo(LearningStatus.NEEDS_REVIEW);
        assertThat(update.nextReviewAt()).isEqualTo(studiedAt.plusDays(1));
    }

    @Test
    void correctAnswerMovesHighScoreToUnderstood() {
        var update = policy.apply(70, LearningEvidence.CORRECT, "정답", studiedAt);

        assertThat(update.score()).isEqualTo(90);
        assertThat(update.status()).isEqualTo(LearningStatus.UNDERSTOOD);
        assertThat(update.nextReviewAt()).isEqualTo(studiedAt.plusDays(7));
    }

    @Test
    void incorrectAnswerRequiresReview() {
        var update = policy.apply(50, LearningEvidence.INCORRECT, "오답", studiedAt);

        assertThat(update.score()).isEqualTo(30);
        assertThat(update.status()).isEqualTo(LearningStatus.NEEDS_REVIEW);
    }

    @Test
    void rejectsInvalidCurrentScore() {
        assertThatThrownBy(() ->
                policy.apply(101, LearningEvidence.DETECTED, "감지", studiedAt)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
