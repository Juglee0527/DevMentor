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
public class OllamaResponseParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public OllamaResponseParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AiTutorResult parse(String responseBody) {
        String content = extractContent(responseBody);
        try {
            AiTutorResponse response = objectMapper.readValue(content, AiTutorResponse.class);
            Set<ConstraintViolation<AiTutorResponse>> violations = validator.validate(response);
            if (!violations.isEmpty()) {
                return fallback(content);
            }
            return new AiTutorResult(response, content, true);
        } catch (JsonProcessingException exception) {
            return fallback(content);
        }
    }

    public AssessmentAiResponse parseAssessment(String responseBody) {
        String content = extractContent(responseBody);
        try {
            AssessmentAiResponse response = objectMapper.readValue(
                    content,
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

    private String extractContent(String responseBody) {
        try {
            String content = objectMapper.readTree(responseBody)
                    .path("message")
                    .path("content")
                    .asText();
            if (!content.isBlank()) {
                return content;
            }
        } catch (JsonProcessingException exception) {
            throw new AiClientException("로컬 AI 응답 형식을 읽지 못했습니다.", exception);
        }
        throw new AiClientException("로컬 AI 응답에 답변 내용이 없습니다.");
    }

    private AiTutorResult fallback(String content) {
        AiTutorResponse fallback = AiTutorResponse.fallback(content);
        if (fallback.answer().isBlank()) {
            throw new AiClientException("로컬 AI 응답에 답변 내용이 없습니다.");
        }
        return new AiTutorResult(fallback, content, false);
    }
}
