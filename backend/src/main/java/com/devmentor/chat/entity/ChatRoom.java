package com.devmentor.chat.entity;

import com.devmentor.common.entity.BaseEntity;
import com.devmentor.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(
        name = "chat_rooms",
        indexes = @Index(name = "idx_chat_room_user_created", columnList = "user_id, created_at")
)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    protected ChatRoom() {
    }

    public ChatRoom(User user, String title) {
        this.user = user;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public void markActive() {
        touch();
    }
}
