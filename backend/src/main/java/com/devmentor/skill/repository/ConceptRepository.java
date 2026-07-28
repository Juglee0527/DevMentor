package com.devmentor.skill.repository;

import com.devmentor.skill.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    Optional<Concept> findBySkillIdAndCode(Long skillId, String code);

    List<Concept> findAllBySkillIdOrderByDisplayOrderAsc(Long skillId);

    @Query("""
            select concept
            from Concept concept
            join fetch concept.skill skill
            where skill.code in :skillCodes
            """)
    List<Concept> findAllBySkillCodeIn(@Param("skillCodes") Set<String> skillCodes);
}
