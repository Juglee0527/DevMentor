package com.devmentor.assessment.service;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.assessment.dto.AssessmentSubmitRequest;
import com.devmentor.assessment.repository.AssessmentRepository;
import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.common.exception.ConflictException;
import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.skill.entity.Concept;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentContextService {

    private final UserService userService;
    private final ChatMessageRepository messageRepository;
    private final ConceptRepository conceptRepository;
    private final UserConceptStatusRepository statusRepository;
    private final AssessmentRepository assessmentRepository;
    private final ObjectMapper objectMapper;

    public AssessmentContextService(
            UserService userService,
            ChatMessageRepository messageRepository,
            ConceptRepository conceptRepository,
            UserConceptStatusRepository statusRepository,
            AssessmentRepository assessmentRepository,
            ObjectMapper objectMapper
    ) {
        this.userService = userService;
        this.messageRepository = messageRepository;
        this.conceptRepository = conceptRepository;
        this.statusRepository = statusRepository;
        this.assessmentRepository = assessmentRepository;
        this.objectMapper = objectMapper;
    }

    public AssessmentPreparation resolve(AssessmentSubmitRequest request) {
        userService.findUser(request.userId());
        ChatMessage message = messageRepository
                .findOwnedById(request.chatMessageId(), request.userId())
                .filter(candidate -> candidate.getRole() == MessageRole.ASSISTANT)
                .orElseThrow(() -> new ResourceNotFoundException("평가 대상 AI 메시지를 찾을 수 없습니다."));
        AiTutorResponse analysis = parseAnalysis(message);
        if (analysis.followUpQuestion() == null || analysis.followUpQuestion().isBlank()) {
            throw new ResourceNotFoundException("평가 대상 확인 질문을 찾을 수 없습니다.");
        }
        if (!containsConcept(analysis, request.skillCode(), request.conceptCode())) {
            throw new ResourceNotFoundException("AI 메시지에서 평가 대상 개념을 찾을 수 없습니다.");
        }

        Concept concept = conceptRepository.findBySkillCodeAndConceptCode(
                        request.skillCode(),
                        request.conceptCode()
                )
                .orElseThrow(() -> new ResourceNotFoundException("평가 대상 개념을 찾을 수 없습니다."));
        if (assessmentRepository.existsByUserIdAndChatMessageIdAndConceptId(
                request.userId(),
                message.getId(),
                concept.getId()
        )) {
            throw new ConflictException("이미 제출한 확인 질문입니다.");
        }
        int currentScore = statusRepository
                .findByUserIdAndConceptId(request.userId(), concept.getId())
                .map(status -> status.getUnderstandingScore())
                .orElse(0);
        return new AssessmentPreparation(
                request.userId(),
                message.getId(),
                concept.getSkill().getCode(),
                concept.getCode(),
                concept.getName(),
                analysis.followUpQuestion(),
                request.userAnswer().trim(),
                currentScore
        );
    }

    private AiTutorResponse parseAnalysis(ChatMessage message) {
        if (message.getAnalysisJson() == null || message.getAnalysisJson().isBlank()) {
            throw new ResourceNotFoundException("AI 메시지에 평가 정보가 없습니다.");
        }
        try {
            return objectMapper.readValue(message.getAnalysisJson(), AiTutorResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ResourceNotFoundException("AI 메시지의 평가 정보를 읽을 수 없습니다.");
        }
    }

    private boolean containsConcept(
            AiTutorResponse analysis,
            String skillCode,
            String conceptCode
    ) {
        return analysis.detectedConcepts().stream().anyMatch(concept ->
                concept.skillCode().equals(skillCode)
                        && concept.conceptCode().equals(conceptCode))
                || analysis.knowledgeGaps().stream().anyMatch(concept ->
                concept.skillCode().equals(skillCode)
                        && concept.conceptCode().equals(conceptCode));
    }
}
