package com.devmentor.assessment.repository;

import com.devmentor.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    List<Assessment> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
