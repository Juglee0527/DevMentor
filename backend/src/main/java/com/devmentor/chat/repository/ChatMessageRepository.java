package com.devmentor.chat.repository;

import com.devmentor.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    List<ChatMessage> findTop10ByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    void deleteAllByChatRoomId(Long chatRoomId);
}
