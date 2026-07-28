package com.devmentor.learning.repository;

import com.devmentor.learning.entity.UserConceptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserConceptStatusRepository extends JpaRepository<UserConceptStatus, Long> {

    Optional<UserConceptStatus> findByUserIdAndConceptId(Long userId, Long conceptId);

    List<UserConceptStatus> findAllByUserId(Long userId);

    @Query("""
            select status
            from UserConceptStatus status
            join fetch status.concept concept
            join fetch concept.skill
            where status.user.id = :userId
            """)
    List<UserConceptStatus> findAllWithConceptByUserId(@Param("userId") Long userId);

    List<UserConceptStatus> findAllByUserIdAndNextReviewAtLessThanEqual(
            Long userId,
            LocalDateTime dateTime
    );
}
