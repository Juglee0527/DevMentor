package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.assessment.dto.AssessmentAiRequest;
import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "openai")
public class OpenAiTutorClient implements AiTutorClient {

    private final OpenAiTransport transport;
    private final OpenAiResponseParser responseParser;
    private final AiTutorPromptBuilder promptBuilder;
    private final AiPromptPolicy promptPolicy;
    private final AiResponseSchemaFactory schemaFactory;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiTutorClient(
            OpenAiTransport transport,
            OpenAiResponseParser responseParser,
            AiTutorPromptBuilder promptBuilder,
            AiPromptPolicy promptPolicy,
            AiResponseSchemaFactory schemaFactory,
            ObjectMapper objectMapper,
            @Value("${app.ai.openai.base-url}") String baseUrl,
            @Value("${app.ai.openai.api-key}") String apiKey,
            @Value("${app.ai.openai.model}") String model,
            @Value("${app.ai.openai.timeout-seconds}") long timeoutSeconds
    ) {
        this.transport = transport;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
        this.promptPolicy = promptPolicy;
        this.schemaFactory = schemaFactory;
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

    @Override
    public AssessmentAiResponse assess(AssessmentAiRequest request) {
        String input;
        try {
            input = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 평가 요청을 생성하지 못했습니다.", exception);
        }
        OpenAiTransport.TransportResponse response = transport.post(
                apiUrl,
                apiKey,
                createRequestBody(
                        promptPolicy.assessmentInstructions(),
                        input,
                        "devmentor_assessment_response",
                        schemaFactory.assessmentSchema()
                ),
                timeout
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiClientException("AI 서비스가 평가 요청을 처리하지 못했습니다.");
        }
        return responseParser.parseAssessment(response.body());
    }

    private String createRequestBody(AiTutorRequest request) {
        return createRequestBody(
                promptPolicy.tutorInstructions(),
                promptBuilder.build(request),
                "devmentor_tutor_response",
                schemaFactory.tutorSchema()
        );
    }

    private String createRequestBody(
            String instructions,
            String input,
            String schemaName,
            JsonNode schema
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("store", false);
        root.put("instructions", instructions);
        root.put("input", input);
        root.set("text", createTextFormat(schemaName, schema));
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 요청을 생성하지 못했습니다.", exception);
        }
    }

    private JsonNode createTextFormat(String schemaName, JsonNode schema) {
        ObjectNode format = objectMapper.createObjectNode();
        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.set("schema", schema);
        return objectMapper.createObjectNode().set("format", format);
    }

    private String requireSetting(String value, String environmentName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentName + " 설정이 필요합니다.");
        }
        return value.trim();
    }
}
