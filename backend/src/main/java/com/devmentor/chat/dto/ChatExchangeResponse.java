package com.devmentor.chat.dto;

import com.devmentor.ai.dto.AiTutorResponse;
import com.devmentor.knowledge.dto.KnowledgeSourceResponse;

import java.util.List;

public record ChatExchangeResponse(
        MessageResponse userMessage,
        MessageResponse assistantMessage,
        AiTutorResponse analysis,
        boolean structured,
        List<KnowledgeSourceResponse> sources
) {
}
