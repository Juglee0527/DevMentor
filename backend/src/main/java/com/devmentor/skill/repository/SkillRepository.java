package com.devmentor.skill.repository;

import com.devmentor.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByCode(String code);

    List<Skill> findAllByOrderByDisplayOrderAsc();

    List<Skill> findAllByCodeIn(Set<String> codes);
}
