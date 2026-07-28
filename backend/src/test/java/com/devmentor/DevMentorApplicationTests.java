package com.devmentor;

import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.skill.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevMentorApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    SkillRepository skillRepository;

    @Autowired
    ConceptRepository conceptRepository;

    @Test
    void initializesRequiredSkillsAndConcepts() {
        assertThat(skillRepository.count()).isEqualTo(9);
        assertThat(conceptRepository.count()).isEqualTo(19);
    }
}
