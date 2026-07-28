package com.devmentor.skill.repository;

import com.devmentor.skill.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    Optional<Concept> findBySkillIdAndCode(Long skillId, String code);

    List<Concept> findAllBySkillIdOrderByDisplayOrderAsc(Long skillId);
}
