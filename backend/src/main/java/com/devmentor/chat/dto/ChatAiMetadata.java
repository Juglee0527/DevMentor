package com.devmentor.chat.dto;

import java.util.List;

public record ChatAiMetadata(
        String provider,
        String model,
        String modelVersion,
        String promptVersion,
        long responseTimeMs,
        String failureType,
        List<String> sourceIds
) {

    public ChatAiMetadata {
        sourceIds = List.copyOf(sourceIds);
    }
}
