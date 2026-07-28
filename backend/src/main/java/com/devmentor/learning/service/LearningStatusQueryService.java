package com.devmentor.learning.service;

import com.devmentor.learning.dto.LearningStatusResponse;
import com.devmentor.learning.entity.LearningStatus;
import com.devmentor.learning.entity.UserConceptStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.skill.entity.Concept;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LearningStatusQueryService {

    private final UserService userService;
    private final ConceptRepository conceptRepository;
    private final UserConceptStatusRepository statusRepository;

    public LearningStatusQueryService(
            UserService userService,
            ConceptRepository conceptRepository,
            UserConceptStatusRepository statusRepository
    ) {
        this.userService = userService;
        this.conceptRepository = conceptRepository;
        this.statusRepository = statusRepository;
    }

    public LearningStatusResponse getStatus(Long userId) {
        userService.findUser(userId);
        List<Concept> concepts = conceptRepository.findAllWithSkillOrderByDisplayOrder();
        Map<Long, UserConceptStatus> statusesByConceptId = statusRepository
                .findAllWithConceptByUserId(userId).stream()
                .collect(Collectors.toMap(
                        status -> status.getConcept().getId(),
                        status -> status
                ));

        Map<Long, SkillAccumulator> skills = new LinkedHashMap<>();
        for (Concept concept : concepts) {
            SkillAccumulator skill = skills.computeIfAbsent(
                    concept.getSkill().getId(),
                    ignored -> new SkillAccumulator(
                            concept.getSkill().getCode(),
                            concept.getSkill().getName()
                    )
            );
            skill.add(toConceptStatus(concept, statusesByConceptId.get(concept.getId())));
        }
        return new LearningStatusResponse(
                skills.values().stream().map(SkillAccumulator::toResponse).toList()
        );
    }

    private LearningStatusResponse.ConceptLearningStatus toConceptStatus(
            Concept concept,
            UserConceptStatus status
    ) {
        if (status == null) {
            return new LearningStatusResponse.ConceptLearningStatus(
                    concept.getCode(),
                    concept.getName(),
                    concept.getDifficulty(),
                    0,
                    LearningStatus.NOT_STARTED,
                    null,
                    null,
                    null
            );
        }
        return new LearningStatusResponse.ConceptLearningStatus(
                concept.getCode(),
                concept.getName(),
                concept.getDifficulty(),
                status.getUnderstandingScore(),
                status.getLearningStatus(),
                status.getAssessmentReason(),
                status.getLastStudiedAt(),
                status.getNextReviewAt()
        );
    }

    private static class SkillAccumulator {

        private final String skillCode;
        private final String skillName;
        private final List<LearningStatusResponse.ConceptLearningStatus> concepts =
                new ArrayList<>();

        private SkillAccumulator(String skillCode, String skillName) {
            this.skillCode = skillCode;
            this.skillName = skillName;
        }

        private void add(LearningStatusResponse.ConceptLearningStatus concept) {
            concepts.add(concept);
        }

        private LearningStatusResponse.SkillLearningStatus toResponse() {
            int averageScore = concepts.isEmpty()
                    ? 0
                    : (int) Math.round(concepts.stream()
                            .mapToInt(LearningStatusResponse.ConceptLearningStatus::understandingScore)
                            .average()
                            .orElse(0));
            return new LearningStatusResponse.SkillLearningStatus(
                    skillCode,
                    skillName,
                    averageScore,
                    List.copyOf(concepts)
            );
        }
    }
}
