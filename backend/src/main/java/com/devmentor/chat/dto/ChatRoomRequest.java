package com.devmentor.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatRoomRequest(
        @NotNull(message = "사용자 ID가 필요합니다.") Long userId,
        @NotBlank(message = "대화방 제목은 비어 있을 수 없습니다.")
        @Size(max = 100, message = "대화방 제목은 100자 이하여야 합니다.")
        String title
) {
}
