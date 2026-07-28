package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OpenAiResponseParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public OpenAiResponseParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AiTutorResult parse(String responseBody) {
        String outputText = extractOutputText(responseBody);
        try {
            AiTutorResponse response = objectMapper.readValue(outputText, AiTutorResponse.class);
            Set<ConstraintViolation<AiTutorResponse>> violations = validator.validate(response);
            if (!violations.isEmpty()) {
                return fallback(outputText);
            }
            return new AiTutorResult(response, outputText, true);
        } catch (JsonProcessingException exception) {
            return fallback(outputText);
        }
    }

    public AssessmentAiResponse parseAssessment(String responseBody) {
        String outputText = extractOutputText(responseBody);
        try {
            AssessmentAiResponse response = objectMapper.readValue(
                    outputText,
                    AssessmentAiResponse.class
            );
            Set<ConstraintViolation<AssessmentAiResponse>> violations =
                    validator.validate(response);
            if (!violations.isEmpty()) {
                throw new AiClientException("AI 평가 응답이 검증 규칙을 충족하지 못했습니다.");
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 평가 응답 형식을 읽지 못했습니다.", exception);
        }
    }

    private String extractOutputText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        String text = content.path("text").asText();
                        if (!text.isBlank()) {
                            return text;
                        }
                    }
                }
            }
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 응답 형식을 읽지 못했습니다.", exception);
        }
        throw new AiClientException("AI 응답에 답변 내용이 없습니다.");
    }

    private AiTutorResult fallback(String outputText) {
        AiTutorResponse fallback = AiTutorResponse.fallback(outputText);
        if (fallback.answer().isBlank()) {
            throw new AiClientException("AI 응답에 답변 내용이 없습니다.");
        }
        return new AiTutorResult(fallback, outputText, false);
    }
}
