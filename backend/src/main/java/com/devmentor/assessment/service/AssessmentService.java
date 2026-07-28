package com.devmentor.assessment.service;

import com.devmentor.ai.client.AiClientException;
import com.devmentor.ai.client.AiTutorClient;
import com.devmentor.assessment.dto.AssessmentAiResponse;
import com.devmentor.assessment.dto.AssessmentResponse;
import com.devmentor.assessment.dto.AssessmentSubmitRequest;
import com.devmentor.assessment.repository.AssessmentRepository;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssessmentService {

    private static final int CORRECT_SCORE_THRESHOLD = 70;

    private final AssessmentContextService contextService;
    private final AssessmentPersistenceService persistenceService;
    private final AiTutorClient aiTutorClient;
    private final UserService userService;
    private final AssessmentRepository assessmentRepository;

    public AssessmentService(
            AssessmentContextService contextService,
            AssessmentPersistenceService persistenceService,
            AiTutorClient aiTutorClient,
            UserService userService,
            AssessmentRepository assessmentRepository
    ) {
        this.contextService = contextService;
        this.persistenceService = persistenceService;
        this.aiTutorClient = aiTutorClient;
        this.userService = userService;
        this.assessmentRepository = assessmentRepository;
    }

    public AssessmentResponse submit(AssessmentSubmitRequest request) {
        AssessmentPreparation preparation = contextService.resolve(request);
        AssessmentAiResponse result = aiTutorClient.assess(preparation.toAiRequest());
        validateConsistency(result);
        return persistenceService.save(preparation, result);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> getAssessments(Long userId) {
        userService.findUser(userId);
        return assessmentRepository.findAllWithDetailsByUserId(userId).stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    private void validateConsistency(AssessmentAiResponse result) {
        boolean scoreMeansCorrect = result.score() >= CORRECT_SCORE_THRESHOLD;
        if (result.correct() != scoreMeansCorrect) {
            throw new AiClientException("AI 평가 결과의 점수와 정오답이 일치하지 않습니다.");
        }
        if (!result.correct() && !result.reviewRequired()) {
            throw new AiClientException("AI 평가 결과의 복습 여부가 정오답과 일치하지 않습니다.");
        }
    }
}
