package com.devmentor.learning.service;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.user.entity.User;
import com.devmentor.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LearningAnalysisIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired LearningAnalysisService analysisService;
    @Autowired LearningRecommendationService recommendationService;
    @Autowired UserRepository userRepository;
    @Autowired UserConceptStatusRepository statusRepository;

    @Test
    void appliesGapOnceAndIgnoresLowConfidenceAndUnknownConcepts() {
        User user = userRepository.save(new User("학습자", 1, "백엔드 개발자", "JPA 학습"));
        AiTutorResponse analysis = new AiTutorResponse(
                "답변",
                List.of(
                        new AiTutorResponse.DetectedConcept("JPA", "ENTITY", 0.9),
                        new AiTutorResponse.DetectedConcept("JPA", "ENTITY", 0.95),
                        new AiTutorResponse.DetectedConcept("JPA", "N_PLUS_ONE", 0.4),
                        new AiTutorResponse.DetectedConcept("UNKNOWN", "UNKNOWN", 0.9)
                ),
                List.of(
                        new AiTutorResponse.KnowledgeGap("JPA", "ENTITY", "엔티티 식별자 이해가 부족합니다.")
                ),
                null,
                List.of()
        );

        LearningAnalysisResult result = analysisService.apply(user.getId(), analysis);

        assertThat(result.updatedCount()).isEqualTo(1);
        assertThat(result.ignoredCount()).isEqualTo(1);
        assertThat(statusRepository.findAllByUserId(user.getId()))
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.getUnderstandingScore()).isZero();
                    assertThat(status.getLearningStatus()).isEqualTo(LearningStatus.NEEDS_REVIEW);
                    assertThat(status.getAssessmentReason()).contains("식별자");
                    assertThat(status.getLastStudiedAt()).isNotNull();
                    assertThat(status.getNextReviewAt()).isAfter(status.getLastStudiedAt());
                });
        assertThat(recommendationService.getRecommendations(user.getId()))
                .singleElement()
                .satisfies(recommendation -> {
                    assertThat(recommendation.conceptCode()).isEqualTo("ENTITY");
                    assertThat(recommendation.learningStatus()).isEqualTo(LearningStatus.NEEDS_REVIEW);
                });
    }
}
