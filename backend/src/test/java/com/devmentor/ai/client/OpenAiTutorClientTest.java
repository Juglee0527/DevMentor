package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiTutorClientTest {

    private ObjectMapper objectMapper;
    private OpenAiResponseParser parser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new OpenAiResponseParser(objectMapper, validator);
    }

    @Test
    void parsesValidStructuredOutput() throws Exception {
        String outputText = """
                {
                  "answer": "영속성 컨텍스트는 엔티티를 관리하는 공간입니다.",
                  "detectedConcepts": [
                    {"skillCode": "JPA", "conceptCode": "PERSISTENCE_CONTEXT", "confidence": 0.9}
                  ],
                  "knowledgeGaps": [],
                  "followUpQuestion": "1차 캐시의 역할은 무엇일까요?",
                  "recommendedConcepts": []
                }
                """;

        AiTutorResult result = parser.parse(apiResponse(outputText));

        assertThat(result.structured()).isTrue();
        assertThat(result.response().answer()).contains("영속성 컨텍스트");
        assertThat(result.response().detectedConcepts()).hasSize(1);
    }

    @Test
    void fallsBackToTextWhenOutputIsNotJson() throws Exception {
        AiTutorResult result = parser.parse(apiResponse("일반 텍스트 답변입니다."));

        assertThat(result.structured()).isFalse();
        assertThat(result.response().answer()).isEqualTo("일반 텍스트 답변입니다.");
        assertThat(result.response().detectedConcepts()).isEmpty();
    }

    @Test
    void fallsBackWhenStructuredFieldsFailValidation() throws Exception {
        String outputText = """
                {
                  "answer": "답변",
                  "detectedConcepts": [
                    {"skillCode": "JPA", "conceptCode": "ENTITY", "confidence": 1.5}
                  ],
                  "knowledgeGaps": [],
                  "followUpQuestion": null,
                  "recommendedConcepts": []
                }
                """;

        AiTutorResult result = parser.parse(apiResponse(outputText));

        assertThat(result.structured()).isFalse();
        assertThat(result.response().detectedConcepts()).isEmpty();
    }

    @Test
    void convertsExternalApiFailureToSafeException() {
        OpenAiTransport failingTransport = (url, apiKey, body, timeout) ->
                new OpenAiTransport.TransportResponse(429, "{\"error\":\"secret detail\"}");
        OpenAiTutorClient client = new OpenAiTutorClient(
                failingTransport,
                parser,
                new AiTutorPromptBuilder(objectMapper),
                objectMapper,
                "https://api.openai.com/v1",
                "test-key",
                "test-model",
                5
        );

        assertThatThrownBy(() -> client.ask(request()))
                .isInstanceOf(AiClientException.class)
                .hasMessage("AI 서비스가 요청을 처리하지 못했습니다.")
                .hasMessageNotContaining("secret detail");
    }

    private String apiResponse(String outputText) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .set("output", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                                .set("content", objectMapper.createArrayNode()
                                        .add(objectMapper.createObjectNode()
                                                .put("type", "output_text")
                                                .put("text", outputText))))));
    }

    private AiTutorRequest request() {
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
