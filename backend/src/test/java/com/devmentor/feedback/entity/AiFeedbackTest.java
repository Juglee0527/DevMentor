package com.devmentor.feedback.entity;

import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.ChatRoom;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiFeedbackTest {

    @Test
    void snapshotsGenerationMetadataAndRevokesTrainingConsent() {
        User user = new User("학습자", 2, "개발자", "JPA 학습");
        ChatRoom room = new ChatRoom(user, "JPA");
        ChatMessage assistant = new ChatMessage(
                room,
                MessageRole.ASSISTANT,
                "검수 전 답변",
                "{}"
        );
        assistant.attachAiMetadata(
                "ollama",
                "qwen2.5:7b-instruct",
                "845dbda0ea48/Q4_K_M",
                "2026-07-29.2",
                1234,
                null,
                "JPA-HIBERNATE-RELATION-001"
        );

        AiFeedback feedback = new AiFeedback(
                user,
                assistant,
                FeedbackRating.NOT_HELPFUL,
                "JPA와 Hibernate의 관계는?",
                assistant.getContent(),
                "JPA는 명세이고 Hibernate는 구현체입니다.",
                true
        );

        assertThat(feedback.getModelProvider()).isEqualTo("ollama");
        assertThat(feedback.getModelName()).isEqualTo("qwen2.5:7b-instruct");
        assertThat(feedback.getModelVersion()).isEqualTo("845dbda0ea48/Q4_K_M");
        assertThat(feedback.getSourceIds()).isEqualTo("JPA-HIBERNATE-RELATION-001");
        assertThat(feedback.isTrainingConsent()).isTrue();

        feedback.revoke();

        assertThat(feedback.isTrainingConsent()).isFalse();
        assertThat(feedback.getDeletedAt()).isNotNull();
    }

    @Test
    void rejectsAiMetadataOnUserMessage() {
        User user = new User("학습자", 2, "개발자", "JPA 학습");
        ChatMessage userMessage = new ChatMessage(
                new ChatRoom(user, "JPA"),
                MessageRole.USER,
                "질문",
                null
        );

        assertThatThrownBy(() -> userMessage.attachAiMetadata(
                "ollama",
                "model",
                "version",
                "prompt",
                1,
                null,
                ""
        )).isInstanceOf(IllegalStateException.class);
    }
}
