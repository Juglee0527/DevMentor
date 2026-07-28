package com.devmentor.chat.repository;

import com.devmentor.chat.entity.ChatMessage;
import com.devmentor.chat.entity.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    List<ChatMessage> findTop10ByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("""
            select message
            from ChatMessage message
            join fetch message.chatRoom room
            where message.id = :messageId
              and room.user.id = :userId
            """)
    Optional<ChatMessage> findOwnedById(
            @Param("messageId") Long messageId,
            @Param("userId") Long userId
    );

    @Query("""
            select message
            from ChatMessage message
            join fetch message.chatRoom room
            where room.user.id = :userId
              and message.role = :role
            order by message.createdAt desc
            """)
    List<ChatMessage> findAllByUserIdAndRoleOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("role") MessageRole role,
            Pageable pageable
    );

    void deleteAllByChatRoomId(Long chatRoomId);
}
