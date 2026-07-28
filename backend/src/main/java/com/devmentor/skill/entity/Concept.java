package com.devmentor.skill.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "concepts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_concept_skill_code",
                columnNames = {"skill_id", "code"}
        ),
        indexes = @Index(name = "idx_concept_skill_order", columnList = "skill_id, display_order")
)
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConceptDifficulty difficulty;

    @Column(nullable = false)
    private int displayOrder;

    protected Concept() {
    }

    public Concept(
            Skill skill,
            String code,
            String name,
            String description,
            ConceptDifficulty difficulty,
            int displayOrder
    ) {
        this.skill = skill;
        this.code = code;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public Skill getSkill() {
        return skill;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ConceptDifficulty getDifficulty() {
        return difficulty;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
