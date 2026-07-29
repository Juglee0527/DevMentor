package com.devmentor.feedback.service;

import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.common.exception.BadRequestException;
import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.feedback.dto.AiFeedbackRequest;
import com.devmentor.feedback.dto.AiFeedbackResponse;
import com.devmentor.feedback.dto.TrainingEligibilityResponse;
import com.devmentor.feedback.entity.AiFeedback;
import com.devmentor.feedback.entity.FeedbackRating;
import com.devmentor.feedback.repository.AiFeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiFeedbackService {

    static final int MINIMUM_CONSENTED_FEEDBACK = 300;
    static final int MINIMUM_CORRECTED_ANSWERS = 200;

    private final AiFeedbackRepository feedbackRepository;
    private final ChatMessageRepository messageRepository;

    public AiFeedbackService(
            AiFeedbackRepository feedbackRepository,
            ChatMessageRepository messageRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public AiFeedbackResponse submit(AiFeedbackRequest request) {
        ChatMessage assistantMessage = messageRepository.findOwnedById(
                        request.chatMessageId(),
                        request.userId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "피드백 대상 AI 답변을 찾을 수 없습니다."
                ));
        if (assistantMessage.getRole() != MessageRole.ASSISTANT) {
            throw new BadRequestException("ASSISTANT 메시지에만 피드백을 남길 수 있습니다.");
        }

        String correctedAnswer = normalize(request.correctedAnswer());
        if (request.trainingConsent()
                && request.rating() == FeedbackRating.NOT_HELPFUL
                && correctedAnswer == null) {
            throw new BadRequestException(
                    "도움이 되지 않은 답변을 학습에 사용할 때는 수정 답안이 필요합니다."
            );
        }

        ChatMessage question = messageRepository
                .findTopByChatRoomIdAndRoleAndCreatedAtBeforeOrderByCreatedAtDesc(
                        assistantMessage.getChatRoom().getId(),
                        MessageRole.USER,
                        assistantMessage.getCreatedAt()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI 답변에 대응하는 사용자 질문을 찾을 수 없습니다."
                ));

        AiFeedback feedback = feedbackRepository
                .findByUserIdAndChatMessageId(request.userId(), request.chatMessageId())
                .orElseGet(() -> new AiFeedback(
                        assistantMessage.getChatRoom().getUser(),
                        assistantMessage,
                        request.rating(),
                        question.getContent(),
                        assistantMessage.getContent(),
                        correctedAnswer,
                        request.trainingConsent()
                ));
        if (feedback.getId() != null) {
            feedback.apply(request.rating(), correctedAnswer, request.trainingConsent());
        }
        return AiFeedbackResponse.from(feedbackRepository.save(feedback));
    }

    @Transactional
    public void revoke(Long feedbackId, Long userId) {
        AiFeedback feedback = feedbackRepository.findByIdAndUserId(feedbackId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "삭제할 AI 피드백을 찾을 수 없습니다."
                ));
        feedback.revoke();
    }

    public TrainingEligibilityResponse getTrainingEligibility() {
        long consentedCount = feedbackRepository
                .countByTrainingConsentTrueAndDeletedAtIsNull();
        long correctedCount = feedbackRepository.countConsentedCorrectedAnswers();
        List<String> blockers = new ArrayList<>();

        if (consentedCount < MINIMUM_CONSENTED_FEEDBACK) {
            blockers.add("학습 동의 피드백이 최소 300건보다 부족합니다.");
        }
        if (correctedCount < MINIMUM_CORRECTED_ANSWERS) {
            blockers.add("검수된 수정 답안이 최소 200건보다 부족합니다.");
        }

        boolean evaluationDatasetReady = true;
        return new TrainingEligibilityResponse(
                blockers.isEmpty() && evaluationDatasetReady,
                consentedCount,
                correctedCount,
                MINIMUM_CONSENTED_FEEDBACK,
                MINIMUM_CORRECTED_ANSWERS,
                evaluationDatasetReady,
                List.copyOf(blockers)
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
