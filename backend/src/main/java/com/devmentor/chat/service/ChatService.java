package com.devmentor.chat.service;

import com.devmentor.chat.dto.*;
import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.ChatRoom;
import com.devmentor.chat.entity.MessageRole;
import com.devmentor.chat.repository.ChatMessageRepository;
import com.devmentor.chat.repository.ChatRoomRepository;
import com.devmentor.common.exception.ResourceNotFoundException;
import com.devmentor.user.entity.User;
import com.devmentor.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository roomRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;

    public ChatService(
            ChatRoomRepository roomRepository,
            ChatMessageRepository messageRepository,
            UserService userService
    ) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @Transactional
    public ChatRoomResponse createRoom(ChatRoomRequest request) {
        User user = userService.findUser(request.userId());
        return ChatRoomResponse.from(
                roomRepository.save(new ChatRoom(user, request.title().trim()))
        );
    }

    public List<ChatRoomResponse> getRooms(Long userId) {
        userService.findUser(userId);
        return roomRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    public ChatRoomResponse getRoom(Long roomId, Long userId) {
        return ChatRoomResponse.from(findOwnedRoom(roomId, userId));
    }

    @Transactional
    public void deleteRoom(Long roomId, Long userId) {
        ChatRoom room = findOwnedRoom(roomId, userId);
        messageRepository.deleteAllByChatRoomId(roomId);
        roomRepository.delete(room);
    }

    @Transactional
    public MessageResponse saveUserMessage(Long roomId, Long userId, MessageRequest request) {
        ChatRoom room = findOwnedRoom(roomId, userId);
        ChatMessage message = new ChatMessage(
                room,
                MessageRole.USER,
                request.content().trim(),
                null
        );
        room.markActive();
        return MessageResponse.from(messageRepository.save(message));
    }

    public List<MessageResponse> getMessages(Long roomId, Long userId) {
        findOwnedRoom(roomId, userId);
        return messageRepository.findAllByChatRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    private ChatRoom findOwnedRoom(Long roomId, Long userId) {
        userService.findUser(userId);
        return roomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("대화방을 찾을 수 없습니다."));
    }
}
