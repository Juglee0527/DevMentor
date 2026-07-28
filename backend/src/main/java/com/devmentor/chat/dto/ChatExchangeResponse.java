package com.devmentor.chat.dto;

import com.devmentor.ai.dto.AiTutorResponse;

public record ChatExchangeResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        AiTutorResponse analysis,
        boolean structured
) {
}
