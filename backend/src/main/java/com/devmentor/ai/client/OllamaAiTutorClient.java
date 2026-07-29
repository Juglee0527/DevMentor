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

    private final OllamaTransport transport;
    private final OllamaResponseParser responseParser;
    private final AiTutorPromptBuilder promptBuilder;
    private final AiPromptPolicy promptPolicy;
    private final AiResponseSchemaFactory schemaFactory;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String model;
    private final Duration timeout;
    private final int maxOutputTokens;

    public OllamaAiTutorClient(
            OllamaTransport transport,
            OllamaResponseParser responseParser,
            AiTutorPromptBuilder promptBuilder,
            AiPromptPolicy promptPolicy,
            AiResponseSchemaFactory schemaFactory,
            ObjectMapper objectMapper,
            @Value("${app.ai.ollama.base-url}") String baseUrl,
            @Value("${app.ai.ollama.model}") String model,
            @Value("${app.ai.ollama.timeout-seconds}") long timeoutSeconds,
            @Value("${app.ai.ollama.max-output-tokens}") int maxOutputTokens
    ) {
        this.transport = transport;
        this.responseParser = responseParser;
        this.promptBuilder = promptBuilder;
        this.promptPolicy = promptPolicy;
        this.schemaFactory = schemaFactory;
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
                createRequestBody(
                        promptPolicy.tutorInstructions(),
                        promptBuilder.build(request),
                        schemaFactory.tutorSchema()
                ),
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
                createRequestBody(
                        promptPolicy.assessmentInstructions(),
                        input,
                        schemaFactory.assessmentSchema()
                ),
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

    private String requireSetting(String value, String environmentName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentName + " 설정이 필요합니다.");
        }
        return value.trim();
    }
}
