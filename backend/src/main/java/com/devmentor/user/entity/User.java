package com.devmentor.user.entity;

import com.devmentor.common.entity.BaseEntity;
import com.devmentor.skill.entity.Skill;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private int careerYears;

    @Column(name = "job_role", nullable = false, length = 100)
    private String currentRole;

    @Column(nullable = false, length = 200)
    private String learningGoal;

    @ManyToMany
    @JoinTable(
            name = "user_interested_skills",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_user_interested_skill",
                    columnNames = {"user_id", "skill_id"}
            )
    )
    @OrderBy("displayOrder ASC")
    private Set<Skill> interestedSkills = new LinkedHashSet<>();

    protected User() {
    }

    public User(String nickname, int careerYears, String currentRole, String learningGoal) {
        this.nickname = nickname;
        this.careerYears = careerYears;
        this.currentRole = currentRole;
        this.learningGoal = learningGoal;
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public int getCareerYears() {
        return careerYears;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public String getLearningGoal() {
        return learningGoal;
    }

    public Set<Skill> getInterestedSkills() {
        return Set.copyOf(interestedSkills);
    }

    public void addInterestedSkill(Skill skill) {
        interestedSkills.add(skill);
    }
}
