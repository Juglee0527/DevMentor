package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorResponse;

public record AiTutorResult(
        AiTutorResponse response,
        String rawText,
        boolean structured
) {
}
