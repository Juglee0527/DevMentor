package com.devmentor.learning.entity;

import com.devmentor.common.entity.BaseEntity;
import com.devmentor.skill.entity.Concept;
import com.devmentor.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_concept_statuses",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_concept_status",
                columnNames = {"user_id", "concept_id"}
        ),
        indexes = @Index(name = "idx_user_concept_review", columnList = "user_id, next_review_at")
)
@Check(constraints = "understanding_score between 0 and 100")
public class UserConceptStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @Column(nullable = false)
    private int understandingScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningStatus learningStatus;

    @Column(length = 1000)
    private String assessmentReason;

    private LocalDateTime lastStudiedAt;

    private LocalDateTime nextReviewAt;

    protected UserConceptStatus() {
    }

    public UserConceptStatus(User user, Concept concept) {
        this.user = user;
        this.concept = concept;
        this.understandingScore = 0;
        this.learningStatus = LearningStatus.NOT_STARTED;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Concept getConcept() {
        return concept;
    }

    public int getUnderstandingScore() {
        return understandingScore;
    }

    public LearningStatus getLearningStatus() {
        return learningStatus;
    }

    public String getAssessmentReason() {
        return assessmentReason;
    }

    public LocalDateTime getLastStudiedAt() {
        return lastStudiedAt;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }
}
