package com.devmentor.ai.service;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.chat.repository.ChatRoomRepository;
import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.learning.repository.UserConceptStatusRepository;
import com.devmentor.knowledge.KnowledgeRetrievalService;
import com.devmentor.skill.repository.ConceptRepository;
import com.devmentor.user.entity.User;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiContextService {

    private static final int RECENT_MESSAGE_LIMIT = 10;

    private final UserService userService;
    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final UserConceptStatusRepository statusRepository;
    private final ConceptRepository conceptRepository;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public AiContextService(
            UserService userService,
            ChatRoomRepository roomRepository,
            ChatMessageRepository messageRepository,
            UserConceptStatusRepository statusRepository,
            ConceptRepository conceptRepository,
            KnowledgeRetrievalService knowledgeRetrievalService
    ) {
        this.userService = userService;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.statusRepository = statusRepository;
        this.conceptRepository = conceptRepository;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    public AiTutorRequest build(Long roomId, Long userId, String currentQuestion) {
        User user = userService.findUser(userId);
        if (!roomRepository.existsByIdAndUserId(roomId, userId)) {
            throw new ResourceNotFoundException("대화방을 찾을 수 없습니다.");
        }

        List<ChatMessage> recentMessages = new ArrayList<>(
                messageRepository.findTop10ByChatRoomIdOrderByCreatedAtDesc(roomId)
        );
        Collections.reverse(recentMessages);

        return new AiTutorRequest(
                new AiTutorRequest.UserContext(
                        user.getNickname(),
                        user.getCareerYears(),
                        user.getCurrentRole(),
                        user.getLearningGoal(),
                        user.getInterestedSkills().stream()
                                .map(skill -> skill.getCode())
                                .toList()
                ),
                currentQuestion,
                recentMessages.stream()
                        .limit(RECENT_MESSAGE_LIMIT)
                        .map(message -> new AiTutorRequest.ConversationMessage(
                                message.getRole().name(),
                                message.getContent()
                        ))
                        .toList(),
                statusRepository.findAllByUserId(userId).stream()
                        .map(status -> new AiTutorRequest.ConceptContext(
                                status.getConcept().getSkill().getCode(),
                                status.getConcept().getCode(),
                                status.getUnderstandingScore(),
                                status.getLearningStatus().name()
                        ))
                        .toList(),
                conceptRepository.findAllWithSkillOrderByDisplayOrder().stream()
                        .map(concept -> new AiTutorRequest.AvailableConcept(
                                concept.getSkill().getCode(),
                                concept.getCode(),
                                concept.getName(),
                                concept.getDifficulty().name()
                        ))
                        .toList(),
                knowledgeRetrievalService.search(currentQuestion).stream()
                        .map(document -> new AiTutorRequest.RetrievedDocument(
                                document.id(),
                                document.title(),
                                document.content(),
                                document.sourceUrl(),
                                document.version()
                        ))
                        .toList()
        );
    }
}
