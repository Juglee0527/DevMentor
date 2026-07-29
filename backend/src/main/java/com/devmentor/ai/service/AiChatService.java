package com.devmentor.ai.service;

import com.devmentor.ai.client.AiTutorClient;
import com.devmentor.ai.client.AiTutorResult;
import com.devmentor.chat.dto.ChatExchangeResponse;
import com.devmentor.chat.dto.ChatAiMetadata;
import com.devmentor.chat.dto.MessageRequest;
import com.devmentor.chat.dto.MessageResponse;
import com.devmentor.chat.service.ChatService;
import com.devmentor.knowledge.dto.KnowledgeSourceResponse;
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
    private final AiResultPersistenceService resultPersistenceService;
    private final ObjectMapper objectMapper;
    private final AiRuntimeDescriptor runtimeDescriptor;

    public AiChatService(
            AiContextService contextService,
            AiTutorClient aiTutorClient,
            ChatService chatService,
            AiResultPersistenceService resultPersistenceService,
            ObjectMapper objectMapper,
            AiRuntimeDescriptor runtimeDescriptor
    ) {
        this.contextService = contextService;
        this.aiTutorClient = aiTutorClient;
        this.chatService = chatService;
        this.resultPersistenceService = resultPersistenceService;
        this.objectMapper = objectMapper;
        this.runtimeDescriptor = runtimeDescriptor;
    }

    public ChatExchangeResponse sendMessage(
            Long roomId,
            Long userId,
            MessageRequest request
    ) {
        String question = request.content().trim();
        var context = contextService.build(roomId, userId, question);
        MessageResponse userMessage = chatService.saveUserMessage(roomId, userId, request);

        long startedAt = System.nanoTime();
        AiTutorResult result = aiTutorClient.ask(context);
        long responseTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
        if (!result.structured()) {
            log.warn("AI structured response validation failed; using text fallback");
        }

        MessageResponse assistantMessage = resultPersistenceService.save(
                roomId,
                userId,
                result.response(),
                serializeAnalysis(result),
                new ChatAiMetadata(
                        runtimeDescriptor.provider(),
                        runtimeDescriptor.model(),
                        runtimeDescriptor.modelVersion(),
                        runtimeDescriptor.promptVersion(),
                        responseTimeMs,
                        result.structured() ? null : "STRUCTURED_FALLBACK",
                        context.retrievedDocuments().stream()
                                .map(document -> document.id())
                                .toList()
                )
        );
        return new ChatExchangeResponse(
                userMessage,
                assistantMessage,
                result.response(),
                result.structured(),
                context.retrievedDocuments().stream()
                        .map(document -> new KnowledgeSourceResponse(
                                document.id(),
                                document.title(),
                                document.sourceUrl(),
                                document.version()
                        ))
                        .toList()
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
