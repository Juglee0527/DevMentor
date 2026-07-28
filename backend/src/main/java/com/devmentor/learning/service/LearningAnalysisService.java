package com.devmentor.learning.service;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.learning.entity.UserConceptStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.skill.entity.Concept;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.user.entity.User;
import com.devmentor.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LearningAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LearningAnalysisService.class);
    private static final double MIN_DETECTION_CONFIDENCE = 0.6;

    private final UserService userService;
    private final ConceptRepository conceptRepository;
    private final UserConceptStatusRepository statusRepository;
    private final LearningProgressPolicy progressPolicy;

    public LearningAnalysisService(
            UserService userService,
            ConceptRepository conceptRepository,
            UserConceptStatusRepository statusRepository,
            LearningProgressPolicy progressPolicy
    ) {
        this.userService = userService;
        this.conceptRepository = conceptRepository;
        this.statusRepository = statusRepository;
        this.progressPolicy = progressPolicy;
    }

    @Transactional
    public LearningAnalysisResult apply(Long userId, AiTutorResponse analysis) {
        User user = userService.findUser(userId);
        Map<ConceptKey, EvidenceSignal> signals = collectSignals(analysis);
        if (signals.isEmpty()) {
            return new LearningAnalysisResult(0, 0);
        }

        Set<String> skillCodes = signals.keySet().stream()
                .map(ConceptKey::skillCode)
                .collect(Collectors.toSet());
        Map<ConceptKey, Concept> concepts = conceptRepository
                .findAllBySkillCodeIn(skillCodes).stream()
                .collect(Collectors.toMap(
                        concept -> new ConceptKey(
                                concept.getSkill().getCode(),
                                concept.getCode()
                        ),
                        concept -> concept
                ));
        Map<Long, UserConceptStatus> statuses = statusRepository
                .findAllWithConceptByUserId(userId).stream()
                .collect(Collectors.toMap(
                        status -> status.getConcept().getId(),
                        status -> status
                ));

        int updatedCount = 0;
        int ignoredCount = 0;
        LocalDateTime studiedAt = LocalDateTime.now();
        for (Map.Entry<ConceptKey, EvidenceSignal> entry : signals.entrySet()) {
            Concept concept = concepts.get(entry.getKey());
            if (concept == null) {
                ignoredCount++;
                log.warn(
                        "Ignoring unknown AI concept code: skill={}, concept={}",
                        entry.getKey().skillCode(),
                        entry.getKey().conceptCode()
                );
                continue;
            }
            UserConceptStatus status = statuses.computeIfAbsent(
                    concept.getId(),
                    ignored -> new UserConceptStatus(user, concept)
            );
            EvidenceSignal signal = entry.getValue();
            LearningProgressPolicy.ProgressUpdate update = progressPolicy.apply(
                    status.getUnderstandingScore(),
                    signal.evidence(),
                    signal.reason(),
                    studiedAt
            );
            status.updateProgress(
                    update.score(),
                    update.status(),
                    update.reason(),
                    update.lastStudiedAt(),
                    update.nextReviewAt()
            );
            statusRepository.save(status);
            updatedCount++;
        }
        return new LearningAnalysisResult(updatedCount, ignoredCount);
    }

    private Map<ConceptKey, EvidenceSignal> collectSignals(AiTutorResponse analysis) {
        Map<ConceptKey, EvidenceSignal> signals = new LinkedHashMap<>();
        analysis.detectedConcepts().stream()
                .filter(detected -> detected.confidence() >= MIN_DETECTION_CONFIDENCE)
                .forEach(detected -> signals.putIfAbsent(
                        new ConceptKey(detected.skillCode(), detected.conceptCode()),
                        new EvidenceSignal(
                                LearningEvidence.DETECTED,
                                String.format(
                                        Locale.ROOT,
                                        "대화에서 개념이 감지되었습니다. (신뢰도 %.2f)",
                                        detected.confidence()
                                )
                        )
                ));
        analysis.knowledgeGaps().forEach(gap -> signals.put(
                new ConceptKey(gap.skillCode(), gap.conceptCode()),
                new EvidenceSignal(LearningEvidence.KNOWLEDGE_GAP, gap.reason())
        ));
        return signals;
    }

    private record ConceptKey(String skillCode, String conceptCode) {
    }

    private record EvidenceSignal(LearningEvidence evidence, String reason) {
    }
}
