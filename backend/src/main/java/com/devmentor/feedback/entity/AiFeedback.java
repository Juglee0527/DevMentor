package com.devmentor.feedback.entity;

import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.common.entity.BaseEntity;
import com.devmentor.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_feedback_user_message",
                columnNames = {"user_id", "chat_message_id"}
        ),
        indexes = @Index(
                name = "idx_ai_feedback_training",
                columnList = "training_consent, deleted_at"
        )
)
public class AiFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackRating rating;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(nullable = false, columnDefinition = "text")
    private String generatedAnswer;

    @Column(columnDefinition = "text")
    private String correctedAnswer;

    @Column(nullable = false)
    private boolean trainingConsent;

    private LocalDateTime consentedAt;

    private LocalDateTime deletedAt;

    @Column(length = 20)
    private String modelProvider;

    @Column(length = 100)
    private String modelName;

    @Column(length = 100)
    private String modelVersion;

    @Column(length = 30)
    private String promptVersion;

    private Long responseTimeMs;

    @Column(length = 50)
    private String failureType;

    @Column(columnDefinition = "text")
    private String sourceIds;

    protected AiFeedback() {
    }

    public AiFeedback(
            User user,
            ChatMessage chatMessage,
            FeedbackRating rating,
            String question,
            String generatedAnswer,
            String correctedAnswer,
            boolean trainingConsent
    ) {
        this.user = user;
        this.chatMessage = chatMessage;
        apply(rating, correctedAnswer, trainingConsent);
        this.question = question;
        this.generatedAnswer = generatedAnswer;
        this.modelProvider = chatMessage.getAiProvider();
        this.modelName = chatMessage.getAiModel();
        this.modelVersion = chatMessage.getAiModelVersion();
        this.promptVersion = chatMessage.getAiPromptVersion();
        this.responseTimeMs = chatMessage.getAiResponseTimeMs();
        this.failureType = chatMessage.getAiFailureType();
        this.sourceIds = chatMessage.getAiSourceIds();
    }

    public void apply(
            FeedbackRating rating,
            String correctedAnswer,
            boolean trainingConsent
    ) {
        this.rating = rating;
        this.correctedAnswer = correctedAnswer;
        this.trainingConsent = trainingConsent;
        this.consentedAt = trainingConsent ? LocalDateTime.now() : null;
        this.deletedAt = null;
    }

    public void revoke() {
        trainingConsent = false;
        consentedAt = null;
        deletedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    public FeedbackRating getRating() {
        return rating;
    }

    public String getQuestion() {
        return question;
    }

    public String getGeneratedAnswer() {
        return generatedAnswer;
    }

    public String getCorrectedAnswer() {
        return correctedAnswer;
    }

    public boolean isTrainingConsent() {
        return trainingConsent;
    }

    public LocalDateTime getConsentedAt() {
        return consentedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public String getModelProvider() {
        return modelProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getFailureType() {
        return failureType;
    }

    public String getSourceIds() {
        return sourceIds;
    }
}
