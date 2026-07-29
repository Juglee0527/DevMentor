package com.devmentor.ai.service;

import com.devmentor.ai.client.AiTutorClient;
import com.devmentor.ai.client.AiTutorResult;
import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.chat.dto.ChatAiMetadata;
import com.devmentor.chat.dto.ChatExchangeResponse;
import com.devmentor.chat.dto.MessageRequest;
import com.devmentor.chat.dto.MessageResponse;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @Test
    void replacesExactDuplicateWithUnexplainedTopicAlternatives() {
        AiContextService contextService = mock(AiContextService.class);
        AiTutorClient aiTutorClient = mock(AiTutorClient.class);
        ChatService chatService = mock(ChatService.class);
        AiResultPersistenceService persistenceService =
                mock(AiResultPersistenceService.class);
        AiRuntimeDescriptor runtimeDescriptor = mock(AiRuntimeDescriptor.class);
        AiTutorRequest context = context();
        AiTutorResponse repeatedResponse = AiTutorResponse.fallback(
                "IoC와 DI는 객체의 의존성을 컨테이너가 주입하는 방식입니다."
        );

        when(contextService.build(1L, 2L, "다른 거 알려줘")).thenReturn(context);
        when(chatService.saveUserMessage(anyLong(), anyLong(), any()))
                .thenReturn(message(10L, MessageRole.USER, "다른 거 알려줘"));
        when(aiTutorClient.ask(context))
                .thenReturn(new AiTutorResult(repeatedResponse, "raw response", true));
        when(persistenceService.save(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(message(11L, MessageRole.ASSISTANT, "대체 답변"));
        when(runtimeDescriptor.provider()).thenReturn("ollama");
        when(runtimeDescriptor.model()).thenReturn("test-model");
        when(runtimeDescriptor.modelVersion()).thenReturn("test-version");
        when(runtimeDescriptor.promptVersion()).thenReturn("test-prompt");

        AiChatService service = new AiChatService(
                contextService,
                aiTutorClient,
                chatService,
                persistenceService,
                new ObjectMapper(),
                runtimeDescriptor
        );

        ChatExchangeResponse response = service.sendMessage(
                1L,
                2L,
                new MessageRequest("다른 거 알려줘")
        );

        ArgumentCaptor<AiTutorResponse> analysisCaptor =
                ArgumentCaptor.forClass(AiTutorResponse.class);
        ArgumentCaptor<ChatAiMetadata> metadataCaptor =
                ArgumentCaptor.forClass(ChatAiMetadata.class);
        verify(persistenceService).save(
                anyLong(),
                anyLong(),
                analysisCaptor.capture(),
                any(),
                metadataCaptor.capture()
        );
        assertThat(response.structured()).isFalse();
        assertThat(analysisCaptor.getValue().answer())
                .contains("AOP", "Bean 생명주기")
                .doesNotContain("IoC와 DI는 객체의 의존성을");
        assertThat(metadataCaptor.getValue().failureType())
                .isEqualTo("DUPLICATE_RESPONSE");
    }

    private AiTutorRequest context() {
        return new AiTutorRequest(
                new AiTutorRequest.UserContext(
                        "학습자",
                        5,
                        "웹 개발자",
                        "백엔드 지식 습득",
                        List.of("SPRING")
                ),
                "다른 거 알려줘",
                List.of(new AiTutorRequest.ConversationMessage(
                        "ASSISTANT",
                        "IoC와 DI는 객체의 의존성을 컨테이너가 주입하는 방식입니다."
                )),
                List.of(),
                List.of(
                        new AiTutorRequest.AvailableConcept(
                                "SPRING",
                                "IOC_DI",
                                "IoC와 DI",
                                "BEGINNER"
                        ),
                        new AiTutorRequest.AvailableConcept(
                                "SPRING",
                                "AOP",
                                "AOP",
                                "INTERMEDIATE"
                        ),
                        new AiTutorRequest.AvailableConcept(
                                "SPRING",
                                "BEAN_LIFECYCLE",
                                "Bean 생명주기",
                                "INTERMEDIATE"
                        )
                ),
                List.of()
        );
    }

    private MessageResponse message(Long id, MessageRole role, String content) {
        return new MessageResponse(id, role, content, LocalDateTime.now());
    }
}
