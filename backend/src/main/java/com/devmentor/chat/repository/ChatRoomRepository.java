package com.devmentor.chat.repository;

import com.devmentor.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    boolean existsByIdAndUserId(Long id, Long userId);

    List<ChatRoom> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ChatRoom> findTop5ByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatRoom> findByIdAndUserId(Long id, Long userId);
}
