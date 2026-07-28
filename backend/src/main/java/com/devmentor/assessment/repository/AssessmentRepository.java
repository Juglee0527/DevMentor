package com.devmentor.assessment.repository;

import com.devmentor.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndChatMessageIdAndConceptId(
            Long userId,
            Long chatMessageId,
            Long conceptId
    );

    @Query("""
            select assessment
            from Assessment assessment
            join fetch assessment.concept concept
            join fetch concept.skill
            join fetch assessment.chatMessage
            where assessment.user.id = :userId
            order by assessment.createdAt desc
            """)
    List<Assessment> findAllWithDetailsByUserId(@Param("userId") Long userId);
}
