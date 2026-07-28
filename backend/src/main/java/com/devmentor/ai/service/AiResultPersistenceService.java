package com.devmentor.ai.service;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.chat.dto.MessageResponse;
import com.devmentor.chat.service.ChatService;
import com.devmentor.learning.service.LearningAnalysisService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiResultPersistenceService {

    private final ChatService chatService;
    private final LearningAnalysisService learningAnalysisService;

    public AiResultPersistenceService(
            ChatService chatService,
            LearningAnalysisService learningAnalysisService
    ) {
        this.chatService = chatService;
        this.learningAnalysisService = learningAnalysisService;
    }

    @Transactional
    public MessageResponse save(
            Long roomId,
            Long userId,
            AiTutorResponse analysis,
            String analysisJson
    ) {
        MessageResponse message = chatService.saveAssistantMessage(
                roomId,
                userId,
                analysis.answer(),
                analysisJson
        );
        learningAnalysisService.apply(userId, analysis);
        return message;
    }
}
