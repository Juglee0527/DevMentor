package com.devmentor.repository;

import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.ChatRoom;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.chat.repository.ChatRoomRepository;
import com.devmentor.learning.entity.UserConceptStatus;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.skill.entity.Concept;
import com.devmentor.skill.entity.Skill;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.skill.repository.SkillRepository;
import com.devmentor.user.entity.User;
import com.devmentor.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Testcontainers
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CoreRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired UserRepository userRepository;
    @Autowired SkillRepository skillRepository;
    @Autowired ConceptRepository conceptRepository;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired UserConceptStatusRepository statusRepository;

    @Test
    void savesAndQueriesCoreLearningFlow() {
        User user = userRepository.save(
                new User("개발자", 3, "백엔드 개발자", "JPA 기초 보완")
        );
        Skill skill = skillRepository.save(new Skill("TEST_JPA", "JPA", "테스트 기술", 1));
        Concept concept = conceptRepository.save(
                new Concept(
                        skill,
                        "TEST_ENTITY",
                        "Entity",
                        "Entity 매핑",
                        com.devmentor.skill.entity.ConceptDifficulty.BEGINNER,
                        1
                )
        );
        user.addInterestedSkill(skill);

        ChatRoom room = chatRoomRepository.save(new ChatRoom(user, "JPA 학습"));
        chatMessageRepository.save(new ChatMessage(room, MessageRole.USER, "Entity가 뭐예요?", null));
        statusRepository.save(new UserConceptStatus(user, concept));

        List<ChatMessage> messages =
                chatMessageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(room.getId());

        assertThat(chatRoomRepository.findByIdAndUserId(room.getId(), user.getId())).isPresent();
        assertThat(messages).extracting(ChatMessage::getContent).containsExactly("Entity가 뭐예요?");
        assertThat(statusRepository.findByUserIdAndConceptId(user.getId(), concept.getId()))
                .isPresent()
                .get()
                .extracting(UserConceptStatus::getUnderstandingScore)
                .isEqualTo(0);
    }
}
