package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.assessment.dto.AssessmentAiRequest;
import com.devmentor.assessment.dto.AssessmentAiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "fake", matchIfMissing = true)
public class FakeAiTutorClient implements AiTutorClient {

    @Override
    public AiTutorResult ask(AiTutorRequest request) {
        String answer = """
                질문하신 내용을 학습 목표에 맞춰 설명드릴게요.

                핵심은 개념의 역할과 실제 사용 시점을 함께 이해하는 것입니다. \
                먼저 작은 예제로 동작을 확인한 뒤, 왜 그런 결과가 나오는지 연결해서 학습해 보세요.
                """;
        List<AiTutorResponse.DetectedConcept> detectedConcepts =
                detectedConcepts(request.currentQuestion());
        AiTutorResponse response = new AiTutorResponse(
                answer,
                detectedConcepts,
                List.of(),
                "이 개념을 직접 사용했던 사례나 예상 동작을 한 문장으로 설명해 보시겠어요?",
                List.of()
        );
        return new AiTutorResult(response, answer, true);
    }

    private List<AiTutorResponse.DetectedConcept> detectedConcepts(String question) {
        if (question.contains("영속성 컨텍스트")) {
            return List.of(new AiTutorResponse.DetectedConcept(
                    "JPA",
                    "PERSISTENCE_CONTEXT",
                    0.95
            ));
        }
        if (question.contains("Hibernate")) {
            return List.of(new AiTutorResponse.DetectedConcept(
                    "JPA",
                    "JPA_HIBERNATE_RELATION",
                    0.9
            ));
        }
        return List.of();
    }

    @Override
    public AssessmentAiResponse assess(AssessmentAiRequest request) {
        String answer = request.userAnswer().toLowerCase();
        boolean correct = answer.contains("표준") && answer.contains("구현");
        if (correct) {
            return new AssessmentAiResponse(
                    true,
                    90,
                    "JPA와 Hibernate의 역할을 올바르게 구분했습니다.",
                    "JPA는 ORM 표준 명세이고 Hibernate는 대표적인 구현체입니다.",
                    false
            );
        }
        return new AssessmentAiResponse(
                false,
                40,
                "핵심 역할의 차이를 조금 더 명확히 구분해 보세요.",
                "JPA는 ORM 표준 명세이고 Hibernate는 대표적인 구현체입니다.",
                true
        );
    }
}
