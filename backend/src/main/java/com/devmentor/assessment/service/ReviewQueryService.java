package com.devmentor.assessment.service;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.assessment.dto.ReviewTargetResponse;
import com.devmentor.assessment.entity.Assessment;
import com.devmentor.assessment.repository.AssessmentRepository;
import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.entity.UserConceptStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class ReviewQueryService {

    private static final Logger log = LoggerFactory.getLogger(ReviewQueryService.class);
    private static final int MESSAGE_SCAN_LIMIT = 100;

    private final UserService userService;
    private final UserConceptStatusRepository statusRepository;
    private final ChatMessageRepository messageRepository;
    private final AssessmentRepository assessmentRepository;
    private final ObjectMapper objectMapper;

    public ReviewQueryService(
            UserService userService,
            UserConceptStatusRepository statusRepository,
            ChatMessageRepository messageRepository,
            AssessmentRepository assessmentRepository,
            ObjectMapper objectMapper
    ) {
        this.userService = userService;
        this.statusRepository = statusRepository;
        this.messageRepository = messageRepository;
        this.assessmentRepository = assessmentRepository;
        this.objectMapper = objectMapper;
    }

    public List<ReviewTargetResponse> getReviewTargets(Long userId) {
        userService.findUser(userId);
        Map<ConceptKey, UserConceptStatus> statuses = statusRepository
                .findAllWithConceptByUserId(userId).stream()
                .filter(status -> status.getUnderstandingScore() < 80)
                .collect(
                        LinkedHashMap::new,
                        (map, status) -> map.put(key(status), status),
                        Map::putAll
                );
        if (statuses.isEmpty()) {
            return List.of();
        }

        Set<AssessmentKey> completed = new HashSet<>();
        for (Assessment assessment : assessmentRepository.findAllWithDetailsByUserId(userId)) {
            completed.add(new AssessmentKey(
                    assessment.getChatMessage().getId(),
                    new ConceptKey(
                            assessment.getConcept().getSkill().getCode(),
                            assessment.getConcept().getCode()
                    )
            ));
        }

        List<ReviewTargetResponse> targets = new ArrayList<>();
        Set<ConceptKey> addedConcepts = new HashSet<>();
        List<ChatMessage> messages = messageRepository
                .findAllByUserIdAndRoleOrderByCreatedAtDesc(
                        userId,
                        MessageRole.ASSISTANT,
                        PageRequest.of(0, MESSAGE_SCAN_LIMIT)
                );
        for (ChatMessage message : messages) {
            AiTutorResponse analysis = parseAnalysis(message);
            if (analysis == null
                    || analysis.followUpQuestion() == null
                    || analysis.followUpQuestion().isBlank()) {
                continue;
            }
            conceptKeys(analysis).forEach(key -> {
                UserConceptStatus status = statuses.get(key);
                AssessmentKey assessmentKey = new AssessmentKey(message.getId(), key);
                if (status == null
                        || completed.contains(assessmentKey)
                        || !addedConcepts.add(key)) {
                    return;
                }
                targets.add(new ReviewTargetResponse(
                        message.getId(),
                        key.skillCode(),
                        status.getConcept().getSkill().getName(),
                        key.conceptCode(),
                        status.getConcept().getName(),
                        analysis.followUpQuestion(),
                        status.getUnderstandingScore(),
                        status.getLearningStatus()
                ));
            });
        }
        return targets.stream()
                .sorted(Comparator
                        .comparingInt((ReviewTargetResponse target) ->
                                target.learningStatus() == LearningStatus.NEEDS_REVIEW ? 0 : 1)
                        .thenComparingInt(ReviewTargetResponse::understandingScore))
                .toList();
    }

    private AiTutorResponse parseAnalysis(ChatMessage message) {
        if (message.getAnalysisJson() == null || message.getAnalysisJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(message.getAnalysisJson(), AiTutorResponse.class);
        } catch (JsonProcessingException exception) {
            log.warn("Skipping invalid AI analysis while building reviews: messageId={}", message.getId());
            return null;
        }
    }

    private Stream<ConceptKey> conceptKeys(AiTutorResponse analysis) {
        Stream<ConceptKey> detected = analysis.detectedConcepts().stream()
                .map(concept -> new ConceptKey(concept.skillCode(), concept.conceptCode()));
        Stream<ConceptKey> gaps = analysis.knowledgeGaps().stream()
                .map(concept -> new ConceptKey(concept.skillCode(), concept.conceptCode()));
        return Stream.concat(detected, gaps).distinct();
    }

    private ConceptKey key(UserConceptStatus status) {
        return new ConceptKey(
                status.getConcept().getSkill().getCode(),
                status.getConcept().getCode()
        );
    }

    private record ConceptKey(String skillCode, String conceptCode) {
    }

    private record AssessmentKey(Long chatMessageId, ConceptKey concept) {
    }
}
