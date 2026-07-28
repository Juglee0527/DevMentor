package com.devmentor.ai.service;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.chat.repository.ChatRoomRepository;
import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.learning.repository.UserConceptStatusRepository;
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

    public AiContextService(
            UserService userService,
            ChatRoomRepository roomRepository,
            ChatMessageRepository messageRepository,
            UserConceptStatusRepository statusRepository
    ) {
        this.userService = userService;
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.statusRepository = statusRepository;
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
                        .toList()
        );
    }
}
