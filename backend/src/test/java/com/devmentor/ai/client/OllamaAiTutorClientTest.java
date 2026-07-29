package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.assessment.dto.AssessmentAiRequest;
import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaAiTutorClientTest {

    private ObjectMapper objectMapper;
    private OllamaResponseParser parser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new OllamaResponseParser(objectMapper, validator);
    }

    @Test
    void sendsStructuredTutorRequestAndParsesResponse() throws Exception {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        AtomicReference<String> requestedBody = new AtomicReference<>();
        OllamaTransport transport = (url, body, timeout) -> {
            requestedUrl.set(url);
            requestedBody.set(body);
            return new OllamaTransport.TransportResponse(200, responseBody("""
                    {
                      "answer": "영속성 컨텍스트는 엔티티를 관리하는 공간입니다.",
                      "detectedConcepts": [
                        {"skillCode": "JPA", "conceptCode": "PERSISTENCE_CONTEXT", "confidence": 0.9}
                      ],
                      "knowledgeGaps": [],
                      "followUpQuestion": "1차 캐시의 역할은 무엇일까요?",
                      "recommendedConcepts": []
                    }
                    """));
        };
        OllamaAiTutorClient client = client(transport);

        AiTutorResult result = client.ask(tutorRequest());

        JsonNode body = objectMapper.readTree(requestedBody.get());
        assertThat(requestedUrl.get()).isEqualTo("http://localhost:11434/api/chat");
        assertThat(body.path("model").asText()).isEqualTo("test-model");
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("think").asBoolean()).isFalse();
        assertThat(body.path("messages").size()).isEqualTo(2);
        assertThat(body.path("format").path("required").size()).isEqualTo(5);
        assertThat(body.path("options").path("temperature").asInt()).isZero();
        assertThat(body.path("options").path("num_predict").asInt()).isEqualTo(1024);
        assertThat(result.structured()).isTrue();
        assertThat(result.response().detectedConcepts()).hasSize(1);
    }

    @Test
    void parsesAssessmentResponse() throws Exception {
        OllamaTransport transport = (url, body, timeout) ->
                new OllamaTransport.TransportResponse(200, responseBody("""
                        {
                          "correct": true,
                          "score": 90,
                          "feedback": "핵심을 정확히 설명했습니다.",
                          "correctAnswer": "JPA는 명세이고 Hibernate는 구현체입니다.",
                          "reviewRequired": false
                        }
                        """));
        OllamaAiTutorClient client = client(transport);

        AssessmentAiResponse result = client.assess(new AssessmentAiRequest(
                "JPA",
                "JPA_HIBERNATE_RELATION",
                "JPA와 Hibernate 관계",
                "둘의 관계는 무엇인가요?",
                "JPA는 명세이고 Hibernate는 구현체입니다.",
                50
        ));

        assertThat(result.correct()).isTrue();
        assertThat(result.score()).isEqualTo(90);
        assertThat(result.reviewRequired()).isFalse();
    }

    @Test
    void fallsBackToTextForNonJsonTutorResponse() throws Exception {
        AiTutorResult result = parser.parse(responseBody("일반 텍스트 답변입니다."));

        assertThat(result.structured()).isFalse();
        assertThat(result.response().answer()).isEqualTo("일반 텍스트 답변입니다.");
    }

    @Test
    void rejectsInvalidAssessmentResponse() throws Exception {
        assertThatThrownBy(() -> parser.parseAssessment(responseBody("""
                {
                  "correct": true,
                  "score": 101,
                  "feedback": "피드백",
                  "correctAnswer": "정답",
                  "reviewRequired": false
                }
                """)))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 평가 응답이 검증 규칙을 충족하지 못했습니다.");
    }

    @Test
    void convertsServerFailureWithoutLeakingResponseBody() {
        OllamaTransport transport = (url, body, timeout) ->
                new OllamaTransport.TransportResponse(404, "{\"error\":\"model secret\"}");
        OllamaAiTutorClient client = client(transport);

        assertThatThrownBy(() -> client.ask(tutorRequest()))
                .isInstanceOf(AiClientException.class)
                .hasMessage("로컬 AI 서버가 요청을 처리하지 못했습니다.")
                .hasMessageNotContaining("model secret");
    }

    @Test
    void requiresModelSetting() {
        OllamaTransport transport = (url, body, timeout) ->
                new OllamaTransport.TransportResponse(200, "{}");

        assertThatThrownBy(() -> new OllamaAiTutorClient(
                transport,
                parser,
                new AiTutorPromptBuilder(objectMapper),
                objectMapper,
                "http://localhost:11434",
                " ",
                5,
                1024
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OLLAMA_MODEL 설정이 필요합니다.");
    }

    private OllamaAiTutorClient client(OllamaTransport transport) {
        return new OllamaAiTutorClient(
                transport,
                parser,
                new AiTutorPromptBuilder(objectMapper),
                objectMapper,
                "http://localhost:11434/",
                "test-model",
                5,
                1024
        );
    }

    private String responseBody(String content) {
        return objectMapper.createObjectNode()
                .set("message", objectMapper.createObjectNode()
                        .put("role", "assistant")
                        .put("content", content))
                .toString();
    }

    private AiTutorRequest tutorRequest() {
        return new AiTutorRequest(
                new AiTutorRequest.UserContext(
                        "학습자",
                        1,
                        "백엔드 개발자",
                        "JPA 학습",
                        List.of("JPA")
                ),
                "영속성 컨텍스트가 무엇인가요?",
                List.of(),
                List.of()
        );
    }
}
