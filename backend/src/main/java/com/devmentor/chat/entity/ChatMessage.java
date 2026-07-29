package com.devmentor.chat.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_messages",
        indexes = @Index(name = "idx_chat_message_room_created", columnList = "chat_room_id, created_at")
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String analysisJson;

    @Column(length = 20)
    private String aiProvider;

    @Column(length = 100)
    private String aiModel;

    @Column(length = 100)
    private String aiModelVersion;

    @Column(length = 30)
    private String aiPromptVersion;

    private Long aiResponseTimeMs;

    @Column(length = 50)
    private String aiFailureType;

    @Column(columnDefinition = "text")
    private String aiSourceIds;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ChatMessage() {
    }

    public ChatMessage(ChatRoom chatRoom, MessageRole role, String content, String analysisJson) {
        this.chatRoom = chatRoom;
        this.role = role;
        this.content = content;
        this.analysisJson = analysisJson;
    }

    public void attachAiMetadata(
            String provider,
            String model,
            String modelVersion,
            String promptVersion,
            long responseTimeMs,
            String failureType,
            String sourceIds
    ) {
        if (role != MessageRole.ASSISTANT) {
            throw new IllegalStateException("AI 메타데이터는 ASSISTANT 메시지에만 저장할 수 있습니다.");
        }
        this.aiProvider = provider;
        this.aiModel = model;
        this.aiModelVersion = modelVersion;
        this.aiPromptVersion = promptVersion;
        this.aiResponseTimeMs = responseTimeMs;
        this.aiFailureType = failureType;
        this.aiSourceIds = sourceIds;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public String getAnalysisJson() {
        return analysisJson;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public String getAiModelVersion() {
        return aiModelVersion;
    }

    public String getAiPromptVersion() {
        return aiPromptVersion;
    }

    public Long getAiResponseTimeMs() {
        return aiResponseTimeMs;
    }

    public String getAiFailureType() {
        return aiFailureType;
    }

    public String getAiSourceIds() {
        return aiSourceIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
