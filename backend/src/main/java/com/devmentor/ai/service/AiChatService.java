package com.devmentor.ai.service;

import com.devmentor.ai.client.AiTutorClient;
import com.devmentor.ai.client.AiTutorResult;
import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.ai.dto.AiTutorResponse;
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

import java.util.List;
import java.util.Locale;

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
        boolean duplicateResponse = isDuplicateResponse(result, context);
        if (duplicateResponse) {
            log.warn("AI repeated the latest assistant answer; using topic alternatives");
            result = duplicateFallback(result, context);
        } else if (!result.structured()) {
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
                        failureType(result, duplicateResponse),
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

    private boolean isDuplicateResponse(AiTutorResult result, AiTutorRequest context) {
        String currentAnswer = normalize(result.response().answer());
        List<AiTutorRequest.ConversationMessage> messages = context.recentMessages();
        for (int index = messages.size() - 1; index >= 0; index--) {
            AiTutorRequest.ConversationMessage message = messages.get(index);
            if ("ASSISTANT".equals(message.role())) {
                return currentAnswer.equals(normalize(message.content()));
            }
        }
        return false;
    }

    private AiTutorResult duplicateFallback(
            AiTutorResult originalResult,
            AiTutorRequest context
    ) {
        String previousAnswers = context.recentMessages().stream()
                .filter(message -> "ASSISTANT".equals(message.role()))
                .map(AiTutorRequest.ConversationMessage::content)
                .map(this::normalize)
                .reduce("", (left, right) -> left + " " + right);
        List<String> interestedSkills = context.user().interestedSkillCodes();
        List<String> alternatives = context.availableConcepts().stream()
                .filter(concept -> interestedSkills.isEmpty()
                        || interestedSkills.contains(concept.skillCode()))
                .filter(concept -> !previousAnswers.contains(normalize(concept.name())))
                .filter(concept -> !previousAnswers.contains(normalize(concept.conceptCode())))
                .map(AiTutorRequest.AvailableConcept::name)
                .distinct()
                .limit(3)
                .toList();

        String answer = alternatives.isEmpty()
                ? "직전 답변과 다른 내용을 원하시는 것으로 이해했습니다. "
                + "원하는 기술이나 개념을 하나만 지정해 주시면 새로운 내용으로 설명드리겠습니다."
                : "직전 답변과 다른 내용을 원하시는 것으로 이해했습니다. "
                + "다음 주제로 이어갈 수 있습니다: "
                + String.join(", ", alternatives)
                + ". 어떤 주제를 먼저 설명해 드릴까요?";
        return new AiTutorResult(
                AiTutorResponse.fallback(answer),
                originalResult.rawText(),
                false
        );
    }

    private String failureType(AiTutorResult result, boolean duplicateResponse) {
        if (duplicateResponse) {
            return "DUPLICATE_RESPONSE";
        }
        return result.structured() ? null : "STRUCTURED_FALLBACK";
    }

    private String normalize(String text) {
        return text == null
                ? ""
                : text.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String serializeAnalysis(AiTutorResult result) {
        try {
            return objectMapper.writeValueAsString(result.response());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 분석 결과를 저장 형식으로 변환하지 못했습니다.", exception);
        }
    }
}
