package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
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
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "openai")
public class OpenAiTutorClient implements AiTutorClient {

    private static final String SYSTEM_INSTRUCTIONS = """
            당신은 사용자의 현재 수준과 학습 목표를 고려해 설명하는 시니어 개발 멘토입니다.
            정확하지 않은 개념 코드는 만들지 말고, 불확실하면 분석 배열에서 제외하세요.
            답변은 구조화 출력 스키마를 따르세요.
            """;

    private final OpenAiTransport transport;
    private final OpenAiResponseParser responseParser;
    private final AiTutorPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiTutorClient(
            OpenAiTransport transport,
            OpenAiResponseParser responseParser,
            AiTutorPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.base-url}") String baseUrl,
            @Value("${app.ai.openai.api-key}") String apiKey,
            @Value("${app.ai.openai.model}") String model,
            @Value("${app.ai.openai.timeout-seconds}") long timeoutSeconds
    ) {
        this.transport = transport;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.apiUrl = baseUrl.replaceAll("/+$", "") + "/responses";
        this.apiKey = requireSetting(apiKey, "OPENAI_API_KEY");
        this.model = requireSetting(model, "OPENAI_MODEL");
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public AiTutorResult ask(AiTutorRequest request) {
        OpenAiTransport.TransportResponse response = transport.post(
                apiUrl,
                apiKey,
                createRequestBody(request),
                timeout
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiClientException("AI 서비스가 요청을 처리하지 못했습니다.");
        }
        return responseParser.parse(response.body());
    }

    private String createRequestBody(AiTutorRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("store", false);
        root.put("instructions", SYSTEM_INSTRUCTIONS);
        root.put("input", promptBuilder.build(request));
        root.set("text", createTextFormat());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 요청을 생성하지 못했습니다.", exception);
        }
    }

    private JsonNode createTextFormat() {
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", "devmentor_tutor_response");
        format.put("strict", true);
        format.set("schema", createResponseSchema());
        return objectMapper.createObjectNode().set("format", format);
    }

    private JsonNode createResponseSchema() {
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
