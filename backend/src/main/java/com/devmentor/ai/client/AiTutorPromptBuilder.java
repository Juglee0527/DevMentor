package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class AiTutorPromptBuilder {

    private final ObjectMapper objectMapper;

    public AiTutorPromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String build(AiTutorRequest request) {
        try {
            return """
                    아래 학습자 문맥을 바탕으로 현재 개발 질문에 한국어로 답변하세요.
                    학습자의 경력과 목표에 맞는 난이도로 핵심 원리와 실용적인 예를 설명하세요.
                    감지하거나 추천하는 개념은 제공된 기술/개념 코드에 확신이 있을 때만 포함하세요.
                    모르는 코드를 만들지 마세요.

                    학습자 문맥:
                    %s
                    """.formatted(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw new AiClientException("AI 요청 문맥을 생성하지 못했습니다.", exception);
        }
    }
}
