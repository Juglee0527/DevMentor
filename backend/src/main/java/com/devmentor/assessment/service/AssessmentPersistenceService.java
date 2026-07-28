package com.devmentor.assessment.service;

import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.devmentor.assessment.dto.AssessmentResponse;
import com.devmentor.assessment.entity.Assessment;
import com.devmentor.assessment.repository.AssessmentRepository;
import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.common.exception.ConflictException;
import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.learning.entity.UserConceptStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.learning.service.LearningEvidence;
import com.devmentor.learning.service.LearningProgressPolicy;
import com.devmentor.skill.entity.Concept;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.user.entity.User;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AssessmentPersistenceService {

    private final UserService userService;
    private final ChatMessageRepository messageRepository;
    private final ConceptRepository conceptRepository;
    private final UserConceptStatusRepository statusRepository;
    private final AssessmentRepository assessmentRepository;
    private final LearningProgressPolicy progressPolicy;

    public AssessmentPersistenceService(
            UserService userService,
            ChatMessageRepository messageRepository,
            ConceptRepository conceptRepository,
            UserConceptStatusRepository statusRepository,
            AssessmentRepository assessmentRepository,
            LearningProgressPolicy progressPolicy
    ) {
        this.userService = userService;
        this.messageRepository = messageRepository;
        this.conceptRepository = conceptRepository;
        this.statusRepository = statusRepository;
        this.assessmentRepository = assessmentRepository;
        this.progressPolicy = progressPolicy;
    }

    @Transactional
    public AssessmentResponse save(
            AssessmentPreparation preparation,
            AssessmentAiResponse result
    ) {
        User user = userService.findUser(preparation.userId());
        ChatMessage message = messageRepository
                .findOwnedById(preparation.chatMessageId(), preparation.userId())
                .orElseThrow(() -> new ResourceNotFoundException("평가 대상 AI 메시지를 찾을 수 없습니다."));
        Concept concept = conceptRepository.findBySkillCodeAndConceptCode(
                        preparation.skillCode(),
                        preparation.conceptCode()
                )
                .orElseThrow(() -> new ResourceNotFoundException("평가 대상 개념을 찾을 수 없습니다."));
        if (assessmentRepository.existsByUserIdAndChatMessageIdAndConceptId(
                user.getId(),
                message.getId(),
                concept.getId()
        )) {
            throw new ConflictException("이미 제출한 확인 질문입니다.");
        }

        UserConceptStatus status = statusRepository
                .findByUserIdAndConceptId(user.getId(), concept.getId())
                .orElseGet(() -> new UserConceptStatus(user, concept));
        var update = progressPolicy.apply(
                status.getUnderstandingScore(),
                result.correct() ? LearningEvidence.CORRECT : LearningEvidence.INCORRECT,
                result.feedback(),
                LocalDateTime.now()
        );
        status.updateProgress(
                update.score(),
                update.status(),
                update.reason(),
                update.lastStudiedAt(),
                update.nextReviewAt()
        );
        statusRepository.save(status);

        Assessment assessment = assessmentRepository.save(new Assessment(
                user,
                concept,
                message,
                preparation.question(),
                preparation.userAnswer(),
                result.score(),
                result.correct(),
                result.feedback(),
                result.correctAnswer(),
                result.reviewRequired()
        ));
        return AssessmentResponse.from(assessment);
    }
}
