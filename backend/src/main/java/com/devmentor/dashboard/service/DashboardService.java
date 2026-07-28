package com.devmentor.dashboard.service;

import com.devmentor.chat.dto.ChatRoomResponse;
import com.devmentor.chat.repository.ChatRoomRepository;
import com.devmentor.dashboard.dto.DashboardResponse;
import com.devmentor.learning.dto.LearningStatusResponse;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.service.LearningStatusQueryService;
import com.devmentor.user.entity.User;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int WEAK_CONCEPT_LIMIT = 5;

    private final UserService userService;
    private final LearningStatusQueryService learningStatusQueryService;
    private final ChatRoomRepository chatRoomRepository;

    public DashboardService(
            UserService userService,
            LearningStatusQueryService learningStatusQueryService,
            ChatRoomRepository chatRoomRepository
    ) {
        this.userService = userService;
        this.learningStatusQueryService = learningStatusQueryService;
        this.chatRoomRepository = chatRoomRepository;
    }

    public DashboardResponse getDashboard(Long userId) {
        User user = userService.findUser(userId);
        LearningStatusResponse learning = learningStatusQueryService.getStatus(userId);
        List<LearningStatusResponse.ConceptLearningStatus> concepts = learning.skills().stream()
                .flatMap(skill -> skill.concepts().stream())
                .toList();
        List<DashboardResponse.WeakConcept> weakConcepts = learning.skills().stream()
                .flatMap(skill -> skill.concepts().stream()
                        .filter(concept -> concept.understandingScore() < 80)
                        .filter(concept -> concept.learningStatus() != LearningStatus.NOT_STARTED)
                        .map(concept -> new DashboardResponse.WeakConcept(
                                skill.skillCode(),
                                skill.skillName(),
                                concept.conceptCode(),
                                concept.conceptName(),
                                concept.understandingScore(),
                                concept.learningStatus(),
                                concept.assessmentReason()
                        )))
                .sorted(Comparator
                        .comparingInt((DashboardResponse.WeakConcept concept) ->
                                concept.learningStatus() == LearningStatus.NEEDS_REVIEW ? 0 : 1)
                        .thenComparingInt(DashboardResponse.WeakConcept::understandingScore))
                .limit(WEAK_CONCEPT_LIMIT)
                .toList();

        int startedCount = (int) concepts.stream()
                .filter(concept -> concept.learningStatus() != LearningStatus.NOT_STARTED)
                .count();
        int overallScore = startedCount == 0
                ? 0
                : (int) Math.round(concepts.stream()
                        .filter(concept -> concept.learningStatus() != LearningStatus.NOT_STARTED)
                        .mapToInt(LearningStatusResponse.ConceptLearningStatus::understandingScore)
                        .average()
                        .orElse(0));
        int reviewTargetCount = (int) concepts.stream()
                .filter(concept -> concept.learningStatus() == LearningStatus.NEEDS_REVIEW)
                .count();

        return new DashboardResponse(
                new DashboardResponse.UserSummary(
                        user.getId(),
                        user.getNickname(),
                        user.getCareerYears(),
                        user.getCurrentRole(),
                        user.getLearningGoal()
                ),
                overallScore,
                concepts.size(),
                startedCount,
                reviewTargetCount,
                learning.skills().stream()
                        .map(skill -> new DashboardResponse.SkillProgress(
                                skill.skillCode(),
                                skill.skillName(),
                                skill.averageScore(),
                                (int) skill.concepts().stream()
                                        .filter(concept ->
                                                concept.learningStatus() != LearningStatus.NOT_STARTED)
                                        .count(),
                                skill.concepts().size()
                        ))
                        .toList(),
                weakConcepts,
                chatRoomRepository.findTop5ByUserIdOrderByUpdatedAtDesc(userId).stream()
                        .map(ChatRoomResponse::from)
                        .toList()
        );
    }
}
