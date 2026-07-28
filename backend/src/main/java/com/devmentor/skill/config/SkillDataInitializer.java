package com.devmentor.skill.config;

import com.devmentor.skill.entity.Concept;
import com.devmentor.skill.entity.ConceptDifficulty;
import com.devmentor.skill.entity.Skill;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.skill.repository.SkillRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SkillDataInitializer implements ApplicationRunner {

    private final SkillRepository skillRepository;
    private final ConceptRepository conceptRepository;

    public SkillDataInitializer(
            SkillRepository skillRepository,
            ConceptRepository conceptRepository
    ) {
        this.skillRepository = skillRepository;
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<SkillSeed> skillSeeds = List.of(
                new SkillSeed("JAVA", "Java", "Java 언어와 JVM 기초", 1),
                new SkillSeed("SPRING", "Spring", "Spring Framework 핵심", 2),
                new SkillSeed("SPRING_BOOT", "Spring Boot", "Spring Boot 애플리케이션 개발", 3),
                new SkillSeed("JPA", "JPA", "JPA와 ORM", 4),
                new SkillSeed("DATABASE", "Database", "관계형 데이터베이스", 5),
                new SkillSeed("REDIS", "Redis", "인메모리 데이터 저장소", 6),
                new SkillSeed("REACT", "React", "React 기반 프론트엔드", 7),
                new SkillSeed("GIT", "Git", "분산 버전 관리", 8),
                new SkillSeed("DOCKER", "Docker", "컨테이너 기반 실행 환경", 9)
        );

        for (SkillSeed seed : skillSeeds) {
            Skill skill = skillRepository.findByCode(seed.code())
                    .orElseGet(() -> skillRepository.save(seed.toEntity()));
            createMissingConcepts(skill, seed.code());
        }
    }

    private void createMissingConcepts(Skill skill, String skillCode) {
        conceptSeeds().stream()
                .filter(seed -> seed.skillCode().equals(skillCode))
                .filter(seed -> conceptRepository.findBySkillIdAndCode(skill.getId(), seed.code()).isEmpty())
                .map(seed -> seed.toEntity(skill))
                .forEach(conceptRepository::save);
    }

    private List<ConceptSeed> conceptSeeds() {
        return List.of(
                new ConceptSeed("JAVA", "OBJECT_REFERENCE", "객체와 참조", ConceptDifficulty.BEGINNER, 1),
                new ConceptSeed("JAVA", "EQUALS_HASHCODE", "equals와 hashCode", ConceptDifficulty.INTERMEDIATE, 2),
                new ConceptSeed("JAVA", "COLLECTION", "Collection", ConceptDifficulty.BEGINNER, 3),
                new ConceptSeed("SPRING", "IOC_DI", "IoC와 DI", ConceptDifficulty.BEGINNER, 1),
                new ConceptSeed("SPRING", "BEAN", "Bean", ConceptDifficulty.BEGINNER, 2),
                new ConceptSeed("SPRING", "TRANSACTION", "Transaction", ConceptDifficulty.INTERMEDIATE, 3),
                new ConceptSeed("SPRING_BOOT", "AUTO_CONFIGURATION", "자동 설정", ConceptDifficulty.INTERMEDIATE, 1),
                new ConceptSeed("JPA", "ENTITY", "Entity", ConceptDifficulty.BEGINNER, 1),
                new ConceptSeed("JPA", "PERSISTENCE_CONTEXT", "영속성 컨텍스트", ConceptDifficulty.INTERMEDIATE, 2),
                new ConceptSeed("JPA", "DIRTY_CHECKING", "Dirty Checking", ConceptDifficulty.INTERMEDIATE, 3),
                new ConceptSeed("JPA", "N_PLUS_ONE", "N+1 문제", ConceptDifficulty.ADVANCED, 4),
                new ConceptSeed("JPA", "JPA_HIBERNATE_RELATION", "JPA와 Hibernate 관계", ConceptDifficulty.BEGINNER, 5),
                new ConceptSeed("DATABASE", "TRANSACTION", "데이터베이스 트랜잭션", ConceptDifficulty.INTERMEDIATE, 1),
                new ConceptSeed("REDIS", "DATA_STRUCTURE", "Redis 자료구조", ConceptDifficulty.INTERMEDIATE, 1),
                new ConceptSeed("REACT", "COMPONENT", "Component", ConceptDifficulty.BEGINNER, 1),
                new ConceptSeed("REACT", "STATE", "State", ConceptDifficulty.BEGINNER, 2),
                new ConceptSeed("REACT", "CONTEXT", "Context", ConceptDifficulty.INTERMEDIATE, 3),
                new ConceptSeed("GIT", "BRANCH", "Branch", ConceptDifficulty.BEGINNER, 1),
                new ConceptSeed("DOCKER", "CONTAINER", "Container", ConceptDifficulty.BEGINNER, 1)
        );
    }

    private record SkillSeed(String code, String name, String description, int order) {
        Skill toEntity() {
            return new Skill(code, name, description, order);
        }
    }

    private record ConceptSeed(
            String skillCode,
            String code,
            String name,
            ConceptDifficulty difficulty,
            int order
    ) {
        Concept toEntity(Skill skill) {
            return new Concept(skill, code, name, name + " 개념을 학습합니다.", difficulty, order);
        }
    }
}
