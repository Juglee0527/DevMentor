package com.devmentor.ai.client;

import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OpenAiResponseParser {

    private final ObjectMapper objectMapper;
    private final AiStructuredContentParser contentParser;

    public OpenAiResponseParser(
            ObjectMapper objectMapper,
            AiStructuredContentParser contentParser
    ) {
        this.objectMapper = objectMapper;
        this.contentParser = contentParser;
    }

    public AiTutorResult parse(String responseBody) {
        return contentParser.parseTutor(extractOutputText(responseBody));
    }

    public AssessmentAiResponse parseAssessment(String responseBody) {
        return contentParser.parseAssessment(extractOutputText(responseBody));
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

}
