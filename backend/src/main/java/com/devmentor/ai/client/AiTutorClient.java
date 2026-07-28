package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;

public interface AiTutorClient {

    AiTutorResult ask(AiTutorRequest request);
}
