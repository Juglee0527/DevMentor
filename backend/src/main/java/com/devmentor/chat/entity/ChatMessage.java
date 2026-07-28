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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
