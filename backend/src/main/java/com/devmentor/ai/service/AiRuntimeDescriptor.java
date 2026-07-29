package com.devmentor.ai.service;

import com.devmentor.ai.client.AiPromptPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRuntimeDescriptor {

    private final String mode;
    private final String openAiModel;
    private final String ollamaModel;
    private final String modelVersion;
    private final AiPromptPolicy promptPolicy;

    public AiRuntimeDescriptor(
            @Value("${app.ai.mode:fake}") String mode,
            @Value("${app.ai.openai.model:}") String openAiModel,
            @Value("${app.ai.ollama.model:}") String ollamaModel,
            @Value("${app.ai.model-version:}") String modelVersion,
            AiPromptPolicy promptPolicy
    ) {
        this.mode = mode;
        this.openAiModel = openAiModel;
        this.ollamaModel = ollamaModel;
        this.modelVersion = modelVersion;
        this.promptPolicy = promptPolicy;
    }

    public String provider() {
        return mode;
    }

    public String model() {
        return switch (mode) {
            case "ollama" -> ollamaModel;
            case "openai" -> openAiModel;
            default -> "deterministic-fake-v1";
        };
    }

    public String promptVersion() {
        return promptPolicy.version();
    }

    public String modelVersion() {
        return modelVersion;
    }
}
