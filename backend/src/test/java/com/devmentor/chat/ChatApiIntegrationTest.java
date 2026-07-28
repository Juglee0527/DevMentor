package com.devmentor.chat;

import com.devmentor.user.entity.User;
import com.devmentor.user.repository.UserRepository;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChatApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserConceptStatusRepository statusRepository;

    @Test
    void createsUserRoomAndMessageAndProtectsOwnership() throws Exception {
        String userBody = """
                {
                  "nickname": "주니어",
                  "careerYears": 2,
                  "currentRole": "백엔드 개발자",
                  "learningGoal": "JPA 학습",
                  "interestedSkillCodes": ["JAVA", "JPA"]
                }
                """;

        String userResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("주니어"))
                .andReturn().getResponse().getContentAsString();

        long userId = extractId(userResponse);
        User otherUser = userRepository.save(new User("다른 사용자", 0, "학생", "Java 학습"));

        String roomResponse = mockMvc.perform(post("/api/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": %d, "title": "JPA 학습"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("JPA 학습"))
                .andReturn().getResponse().getContentAsString();

        long roomId = extractId(roomResponse);

        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", roomId)
                        .queryParam("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hibernate가 뭐예요?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.analysis.followUpQuestion").isNotEmpty())
                .andExpect(jsonPath("$.data.structured").value(true));

        mockMvc.perform(get("/api/chat-rooms/{roomId}/messages", roomId)
                        .queryParam("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Hibernate가 뭐예요?"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"));

        assertThat(statusRepository.findAllWithConceptByUserId(userId))
                .singleElement()
                .satisfies(status -> {
                    assertThat(status.getConcept().getCode()).isEqualTo("JPA_HIBERNATE_RELATION");
                    assertThat(status.getUnderstandingScore()).isEqualTo(10);
                });

        mockMvc.perform(get("/api/learning/recommendations")
                        .queryParam("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].conceptCode").value("JPA_HIBERNATE_RELATION"))
                .andExpect(jsonPath("$.data[0].understandingScore").value(10));

        mockMvc.perform(get("/api/chat-rooms/{roomId}", roomId)
                        .queryParam("userId", String.valueOf(otherUser.getId())))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", roomId)
                        .queryParam("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/chat-rooms/{roomId}", roomId)
                        .queryParam("userId", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    private long extractId(String json) {
        String marker = "\"id\":";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf(',', start);
        return Long.parseLong(json.substring(start, end));
    }
}
