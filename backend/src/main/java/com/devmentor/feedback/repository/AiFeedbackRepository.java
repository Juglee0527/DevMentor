package com.devmentor.feedback.repository;

import com.devmentor.feedback.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findByUserIdAndChatMessageId(Long userId, Long chatMessageId);

    Optional<AiFeedback> findByIdAndUserId(Long id, Long userId);

    long countByTrainingConsentTrueAndDeletedAtIsNull();

    @Query("""
            select count(feedback)
            from AiFeedback feedback
            where feedback.trainingConsent = true
              and feedback.deletedAt is null
              and feedback.correctedAnswer is not null
              and feedback.correctedAnswer <> ''
            """)
    long countConsentedCorrectedAnswers();
}
