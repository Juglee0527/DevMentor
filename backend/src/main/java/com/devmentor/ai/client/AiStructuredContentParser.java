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
public class AiStructuredContentParser {

    private static final int CORRECT_SCORE_THRESHOLD = 70;

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AiStructuredContentParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public AiTutorResult parseTutor(String content) {
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

    public AssessmentAiResponse parseAssessment(String content) {
        try {
            AssessmentAiResponse response = objectMapper.readValue(
                    content,
                    AssessmentAiResponse.class
            );
            Set<ConstraintViolation<AssessmentAiResponse>> violations =
                    validator.validate(response);
            if (!violations.isEmpty() || hasInconsistentAssessment(response)) {
                throw new AiClientException("AI 평가 응답이 검증 규칙을 충족하지 못했습니다.");
            }
            return response;
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 평가 응답 형식을 읽지 못했습니다.", exception);
        }
    }

    private boolean hasInconsistentAssessment(AssessmentAiResponse response) {
        boolean scoreSaysCorrect = response.score() >= CORRECT_SCORE_THRESHOLD;
        return response.correct() != scoreSaysCorrect
                || (!response.correct() && !response.reviewRequired());
    }

    private AiTutorResult fallback(String content) {
        AiTutorResponse fallback = AiTutorResponse.fallback(extractAnswer(content));
        if (fallback.answer().isBlank()) {
            throw new AiClientException("AI 응답에 답변 내용이 없습니다.");
        }
        return new AiTutorResult(fallback, content, false);
    }

    private String extractAnswer(String content) {
        try {
            JsonNode parsedContent = objectMapper.readTree(content);
            if (parsedContent != null) {
                String answer = parsedContent.path("answer").asText();
                if (!answer.isBlank()) {
                    return answer;
                }
            }
        } catch (JsonProcessingException ignored) {
            // 구조화되지 않은 일반 텍스트는 기존 fallback 경로로 처리합니다.
        }
        return content;
    }
}
