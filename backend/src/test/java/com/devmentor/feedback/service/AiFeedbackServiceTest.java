package com.devmentor.feedback.service;

import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.feedback.dto.TrainingEligibilityResponse;
import com.devmentor.feedback.repository.AiFeedbackRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiFeedbackServiceTest {

    private final AiFeedbackRepository feedbackRepository =
            mock(AiFeedbackRepository.class);
    private final AiFeedbackService service = new AiFeedbackService(
            feedbackRepository,
            mock(ChatMessageRepository.class)
    );

    @Test
    void blocksTrainingWhenConsentedAndCorrectedDataAreInsufficient() {
        when(feedbackRepository.countByTrainingConsentTrueAndDeletedAtIsNull())
                .thenReturn(299L);
        when(feedbackRepository.countConsentedCorrectedAnswers()).thenReturn(199L);

        TrainingEligibilityResponse result = service.getTrainingEligibility();

        assertThat(result.eligible()).isFalse();
        assertThat(result.blockers()).hasSize(2);
        assertThat(result.minimumConsentedFeedback()).isEqualTo(300);
        assertThat(result.minimumCorrectedAnswers()).isEqualTo(200);
    }

    @Test
    void allowsTrainingPreparationOnlyAfterEveryGatePasses() {
        when(feedbackRepository.countByTrainingConsentTrueAndDeletedAtIsNull())
                .thenReturn(300L);
        when(feedbackRepository.countConsentedCorrectedAnswers()).thenReturn(200L);

        TrainingEligibilityResponse result = service.getTrainingEligibility();

        assertThat(result.eligible()).isTrue();
        assertThat(result.blockers()).isEmpty();
        assertThat(result.separateEvaluationDatasetReady()).isTrue();
    }
}
