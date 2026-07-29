package com.devmentor.ai.client;

import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OllamaResponseParser {

    private final ObjectMapper objectMapper;
    private final AiStructuredContentParser contentParser;

    public OllamaResponseParser(
            ObjectMapper objectMapper,
            AiStructuredContentParser contentParser
    ) {
        this.objectMapper = objectMapper;
        this.contentParser = contentParser;
    }

    public AiTutorResult parse(String responseBody) {
        return contentParser.parseTutor(extractContent(responseBody));
    }

    public AssessmentAiResponse parseAssessment(String responseBody) {
        return contentParser.parseAssessment(extractContent(responseBody));
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

}
