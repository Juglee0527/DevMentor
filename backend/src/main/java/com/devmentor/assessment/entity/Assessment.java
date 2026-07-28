package com.devmentor.assessment.entity;

import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.skill.entity.Concept;
import com.devmentor.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "assessments",
        indexes = @Index(name = "idx_assessment_user_created", columnList = "user_id, created_at")
)
@Check(constraints = "score between 0 and 100")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, columnDefinition = "text")
    private String userAnswer;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false, columnDefinition = "text")
    private String feedback;

    @Column(nullable = false, columnDefinition = "text")
    private String correctAnswer;

    @Column(nullable = false)
    private boolean reviewRequired;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Assessment() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
