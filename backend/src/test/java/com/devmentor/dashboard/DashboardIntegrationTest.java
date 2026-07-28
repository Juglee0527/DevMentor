package com.devmentor.dashboard;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.chat.dto.ChatRoomRequest;
import com.devmentor.chat.service.ChatService;
import com.devmentor.dashboard.service.DashboardService;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.service.LearningAnalysisService;
import com.devmentor.learning.service.LearningStatusQueryService;
import com.devmentor.user.entity.User;
import com.devmentor.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
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
class DashboardIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired DashboardService dashboardService;
    @Autowired LearningStatusQueryService statusQueryService;
    @Autowired LearningAnalysisService analysisService;
    @Autowired ChatService chatService;
    @Autowired UserRepository userRepository;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void returnsCompleteEmptyStateWithoutRepeatedQueries() {
        User user = userRepository.save(new User("새 학습자", 0, "학생", "개발 기초"));
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        sessionFactory.getStatistics().clear();

        var dashboard = dashboardService.getDashboard(user.getId());
        long queryCount = sessionFactory.getStatistics().getPrepareStatementCount();
        var learning = statusQueryService.getStatus(user.getId());

        assertThat(dashboard.overallUnderstandingScore()).isZero();
        assertThat(dashboard.startedConceptCount()).isZero();
        assertThat(dashboard.totalConceptCount()).isEqualTo(19);
        assertThat(dashboard.skillProgress()).hasSize(9);
        assertThat(dashboard.weakConcepts()).isEmpty();
        assertThat(dashboard.recentChats()).isEmpty();
        assertThat(learning.skills()).hasSize(9);
        assertThat(learning.skills())
                .flatExtracting(skill -> skill.concepts())
                .hasSize(19)
                .allSatisfy(concept ->
                        assertThat(concept.learningStatus()).isEqualTo(LearningStatus.NOT_STARTED));
        assertThat(queryCount).isLessThanOrEqualTo(4);
    }

    @Test
    void aggregatesStoredLearningStatusAndRecentChat() {
        User user = userRepository.save(new User("학습자", 2, "백엔드 개발자", "JPA 학습"));
        analysisService.apply(user.getId(), new AiTutorResponse(
                "답변",
                List.of(new AiTutorResponse.DetectedConcept(
                        "JPA",
                        "PERSISTENCE_CONTEXT",
                        0.95
                )),
                List.of(),
                null,
                List.of()
        ));
        chatService.createRoom(new ChatRoomRequest(user.getId(), "영속성 컨텍스트"));

        var dashboard = dashboardService.getDashboard(user.getId());

        assertThat(dashboard.overallUnderstandingScore()).isEqualTo(10);
        assertThat(dashboard.startedConceptCount()).isEqualTo(1);
        assertThat(dashboard.weakConcepts())
                .singleElement()
                .satisfies(concept -> {
                    assertThat(concept.skillCode()).isEqualTo("JPA");
                    assertThat(concept.conceptCode()).isEqualTo("PERSISTENCE_CONTEXT");
                });
        assertThat(dashboard.recentChats())
                .singleElement()
                .satisfies(chat -> assertThat(chat.title()).isEqualTo("영속성 컨텍스트"));
    }
}
