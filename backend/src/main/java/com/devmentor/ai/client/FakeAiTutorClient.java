package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.ai.dto.AiTutorResponse;
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
        AiTutorResponse response = new AiTutorResponse(
                answer,
                List.of(),
                List.of(),
                "이 개념을 직접 사용했던 사례나 예상 동작을 한 문장으로 설명해 보시겠어요?",
                List.of()
        );
        return new AiTutorResult(response, answer, true);
    }
}
