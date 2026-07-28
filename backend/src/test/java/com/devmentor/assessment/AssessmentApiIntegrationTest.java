package com.devmentor.assessment;

import com.devmentor.assessment.repository.AssessmentRepository;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AssessmentApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AssessmentRepository assessmentRepository;
    @Autowired UserConceptStatusRepository statusRepository;

    @Test
    void evaluatesAnswersPreventsDuplicatesAndUpdatesReviewState() throws Exception {
        long userId = createUser();
        long roomId = createRoom(userId);
        long firstAssistantMessageId = askQuestion(roomId, userId);

        mockMvc.perform(get("/api/reviews").queryParam("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].chatMessageId").value(firstAssistantMessageId))
                .andExpect(jsonPath("$.data[0].conceptCode").value("JPA_HIBERNATE_RELATION"));

        String wrongRequest = assessmentRequest(
                userId,
                firstAssistantMessageId,
                "둘은 같은 라이브러리입니다."
        );
        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(40))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.reviewRequired").value(true));

        var statusAfterWrong = statusRepository.findAllWithConceptByUserId(userId)
                .getFirst();
        assertThat(statusAfterWrong.getUnderstandingScore()).isZero();
        assertThat(statusAfterWrong.getLearningStatus()).isEqualTo(LearningStatus.NEEDS_REVIEW);

        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 제출한 확인 질문입니다."));

        long secondAssistantMessageId = askQuestion(roomId, userId);
        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentRequest(
                                userId,
                                secondAssistantMessageId,
                                "JPA는 ORM 표준 명세이고 Hibernate는 그 구현체입니다."
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(90))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.reviewRequired").value(false));

        var statusAfterCorrect = statusRepository.findAllWithConceptByUserId(userId)
                .getFirst();
        assertThat(statusAfterCorrect.getUnderstandingScore()).isEqualTo(30);
        assertThat(statusAfterCorrect.getLearningStatus()).isEqualTo(LearningStatus.LEARNING);
        assertThat(assessmentRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).hasSize(2);

        mockMvc.perform(get("/api/assessments")
                        .queryParam("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentRequest(userId, secondAssistantMessageId, "  ")))
                .andExpect(status().isBadRequest());
    }

    private long createUser() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "평가 학습자",
                                  "careerYears": 2,
                                  "currentRole": "백엔드 개발자",
                                  "learningGoal": "JPA 학습",
                                  "interestedSkillCodes": ["JPA"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long createRoom(long userId) throws Exception {
        String response = mockMvc.perform(post("/api/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "title": "JPA 평가"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long askQuestion(long roomId, long userId) throws Exception {
        String response = mockMvc.perform(post(
                                "/api/chat-rooms/{roomId}/messages",
                                roomId
                        )
                        .queryParam("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hibernate가 뭐예요?\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response)
                .path("data")
                .path("assistantMessage")
                .path("id")
                .asLong();
    }

    private String assessmentRequest(long userId, long messageId, String answer)
            throws Exception {
        JsonNode body = objectMapper.createObjectNode()
                .put("userId", userId)
                .put("chatMessageId", messageId)
                .put("skillCode", "JPA")
                .put("conceptCode", "JPA_HIBERNATE_RELATION")
                .put("userAnswer", answer);
        return objectMapper.writeValueAsString(body);
    }
}
