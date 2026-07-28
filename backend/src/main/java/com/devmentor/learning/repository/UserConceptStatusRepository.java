package com.devmentor.learning.repository;

import com.devmentor.learning.entity.UserConceptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserConceptStatusRepository extends JpaRepository<UserConceptStatus, Long> {

    Optional<UserConceptStatus> findByUserIdAndConceptId(Long userId, Long conceptId);

    List<UserConceptStatus> findAllByUserId(Long userId);

    List<UserConceptStatus> findAllByUserIdAndNextReviewAtLessThanEqual(
            Long userId,
            LocalDateTime dateTime
    );
}
