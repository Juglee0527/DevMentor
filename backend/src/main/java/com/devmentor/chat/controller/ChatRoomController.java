package com.devmentor.chat.controller;

import com.devmentor.chat.dto.*;
import com.devmentor.chat.service.ChatService;
import com.devmentor.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final ChatService chatService;

    public ChatRoomController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ApiResponse<ChatRoomResponse> create(@Valid @RequestBody ChatRoomRequest request) {
        return ApiResponse.success("대화방을 생성했습니다.", chatService.createRoom(request));
    }

    @GetMapping
    public ApiResponse<List<ChatRoomResponse>> getRooms(@RequestParam Long userId) {
        return ApiResponse.success("대화방 목록을 조회했습니다.", chatService.getRooms(userId));
    }

    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomResponse> getRoom(
            @PathVariable Long roomId,
            @RequestParam Long userId
    ) {
        return ApiResponse.success("대화방을 조회했습니다.", chatService.getRoom(roomId, userId));
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> delete(
            @PathVariable Long roomId,
            @RequestParam Long userId
    ) {
        chatService.deleteRoom(roomId, userId);
        return ApiResponse.success("대화방을 삭제했습니다.", null);
    }

    @PostMapping("/{roomId}/messages")
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable Long roomId,
            @RequestParam Long userId,
            @Valid @RequestBody MessageRequest request
    ) {
        return ApiResponse.success(
                "메시지를 저장했습니다.",
                chatService.saveUserMessage(roomId, userId, request)
        );
    }

    @GetMapping("/{roomId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam Long userId
    ) {
        return ApiResponse.success(
                "메시지 목록을 조회했습니다.",
                chatService.getMessages(roomId, userId)
        );
    }
}
