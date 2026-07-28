package com.devmentor.ai.service;

import com.devmentor.ai.client.AiTutorClient;
import com.devmentor.ai.client.AiTutorResult;
import com.devmentor.chat.dto.ChatExchangeResponse;
import com.devmentor.chat.dto.MessageRequest;
import com.devmentor.chat.dto.MessageResponse;
import com.devmentor.chat.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final AiContextService contextService;
    private final AiTutorClient aiTutorClient;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public AiChatService(
            AiContextService contextService,
            AiTutorClient aiTutorClient,
            ChatService chatService,
            ObjectMapper objectMapper
    ) {
        this.contextService = contextService;
        this.aiTutorClient = aiTutorClient;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    public ChatExchangeResponse sendMessage(
            Long roomId,
            Long userId,
            MessageRequest request
    ) {
        String question = request.content().trim();
        var context = contextService.build(roomId, userId, question);
        MessageResponse userMessage = chatService.saveUserMessage(roomId, userId, request);

        AiTutorResult result = aiTutorClient.ask(context);
        if (!result.structured()) {
            log.warn("AI structured response validation failed; using text fallback");
        }

        MessageResponse assistantMessage = chatService.saveAssistantMessage(
                roomId,
                userId,
                result.response().answer(),
                serializeAnalysis(result)
        );
        return new ChatExchangeResponse(
                userMessage,
                assistantMessage,
                result.response(),
                result.structured()
        );
    }

    private String serializeAnalysis(AiTutorResult result) {
        try {
            return objectMapper.writeValueAsString(result.response());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 분석 결과를 저장 형식으로 변환하지 못했습니다.", exception);
        }
    }
}
