package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.assessment.dto.AssessmentAiRequest;
import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "ollama")
public class OllamaAiTutorClient implements AiTutorClient {

    private static final String SYSTEM_INSTRUCTIONS = """
            당신은 사용자의 현재 수준과 학습 목표를 고려해 설명하는 시니어 개발 멘토입니다.
            정확하지 않은 개념 코드는 만들지 말고, 불확실하면 분석 배열에서 제외하세요.
            답변은 구조화 출력 스키마를 따르세요.
            """;
    private static final String ASSESSMENT_INSTRUCTIONS = """
            당신은 개발 개념 확인 질문의 답변을 평가하는 시니어 개발 멘토입니다.
            질문과 개념에 근거해 0부터 100까지 점수, 정오답, 구체적 피드백, 모범 답안,
            복습 필요 여부를 구조화 출력 스키마에 맞춰 반환하세요.
            """;

    private final OllamaTransport transport;
    private final OllamaResponseParser responseParser;
    private final AiTutorPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String model;
    private final Duration timeout;
    private final int maxOutputTokens;

    public OllamaAiTutorClient(
            OllamaTransport transport,
            OllamaResponseParser responseParser,
            AiTutorPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            @Value("${app.ai.ollama.base-url}") String baseUrl,
            @Value("${app.ai.ollama.model}") String model,
            @Value("${app.ai.ollama.timeout-seconds}") long timeoutSeconds,
            @Value("${app.ai.ollama.max-output-tokens}") int maxOutputTokens
    ) {
        this.transport = transport;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.apiUrl = requireSetting(baseUrl, "OLLAMA_BASE_URL")
                .replaceAll("/+$", "") + "/api/chat";
        this.model = requireSetting(model, "OLLAMA_MODEL");
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        if (maxOutputTokens < 1) {
            throw new IllegalStateException("OLLAMA_MAX_OUTPUT_TOKENS는 1 이상이어야 합니다.");
        }
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public AiTutorResult ask(AiTutorRequest request) {
        OllamaTransport.TransportResponse response = transport.post(
                apiUrl,
                createRequestBody(SYSTEM_INSTRUCTIONS, promptBuilder.build(request), responseSchema()),
                timeout
        );
        ensureSuccess(response);
        return responseParser.parse(response.body());
    }

    @Override
    public AssessmentAiResponse assess(AssessmentAiRequest request) {
        String input;
        try {
            input = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 평가 요청을 생성하지 못했습니다.", exception);
        }
        OllamaTransport.TransportResponse response = transport.post(
                apiUrl,
                createRequestBody(ASSESSMENT_INSTRUCTIONS, input, assessmentSchema()),
                timeout
        );
        ensureSuccess(response);
        return responseParser.parseAssessment(response.body());
    }

    private String createRequestBody(String instructions, String input, JsonNode schema) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", false);
        root.put("think", false);
        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", instructions);
        messages.addObject()
                .put("role", "user")
                .put("content", input);
        root.set("format", schema);
        root.putObject("options")
                .put("temperature", 0)
                .put("num_predict", maxOutputTokens);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("로컬 AI 요청을 생성하지 못했습니다.", exception);
        }
    }

    private void ensureSuccess(OllamaTransport.TransportResponse response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiClientException("로컬 AI 서버가 요청을 처리하지 못했습니다.");
        }
    }

    private JsonNode responseSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("answer", stringSchema());
        properties.set("detectedConcepts", arraySchema(detectedConceptSchema()));
        properties.set("knowledgeGaps", arraySchema(knowledgeGapSchema()));
        properties.set("followUpQuestion", nullableStringSchema());
        properties.set("recommendedConcepts", arraySchema(recommendedConceptSchema()));
        schema.set("required", stringArray(
                "answer",
                "detectedConcepts",
                "knowledgeGaps",
                "followUpQuestion",
                "recommendedConcepts"
        ));
        return schema;
    }

    private JsonNode assessmentSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("correct", objectMapper.createObjectNode().put("type", "boolean"));
        properties.set("score", objectMapper.createObjectNode()
                .put("type", "integer")
                .put("minimum", 0)
                .put("maximum", 100));
        properties.set("feedback", stringSchema());
        properties.set("correctAnswer", stringSchema());
        properties.set("reviewRequired", objectMapper.createObjectNode().put("type", "boolean"));
        schema.set("required", stringArray(
                "correct",
                "score",
                "feedback",
                "correctAnswer",
                "reviewRequired"
        ));
        return schema;
    }

    private ObjectNode detectedConceptSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("skillCode", stringSchema());
        properties.set("conceptCode", stringSchema());
        properties.set("confidence", objectMapper.createObjectNode()
                .put("type", "number")
                .put("minimum", 0)
                .put("maximum", 1));
        schema.set("required", stringArray("skillCode", "conceptCode", "confidence"));
        return schema;
    }

    private ObjectNode knowledgeGapSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("skillCode", stringSchema());
        properties.set("conceptCode", stringSchema());
        properties.set("reason", stringSchema());
        schema.set("required", stringArray("skillCode", "conceptCode", "reason"));
        return schema;
    }

    private ObjectNode recommendedConceptSchema() {
        ObjectNode schema = objectSchema();
        ObjectNode properties = schema.putObject("properties");
        properties.set("skillCode", stringSchema());
        properties.set("conceptCode", stringSchema());
        properties.set("reason", stringSchema());
        schema.set("required", stringArray("skillCode", "conceptCode", "reason"));
        return schema;
    }

    private ObjectNode objectSchema() {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .put("additionalProperties", false);
    }

    private ObjectNode stringSchema() {
        return objectMapper.createObjectNode().put("type", "string");
    }

    private ObjectNode nullableStringSchema() {
        ArrayNode types = objectMapper.createArrayNode().add("string").add("null");
        return objectMapper.createObjectNode().set("type", types);
    }

    private ObjectNode arraySchema(JsonNode itemSchema) {
        return objectMapper.createObjectNode()
                .put("type", "array")
                .set("items", itemSchema);
    }

    private ArrayNode stringArray(String... values) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private String requireSetting(String value, String environmentName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentName + " 설정이 필요합니다.");
        }
        return value.trim();
    }
}
