package com.devmentor.ai.client;

import com.devmentor.ai.dto.AiTutorRequest;
import com.devmentor.assessment.dto.AssessmentAiRequest;
import com.devmentor.assessment.dto.AssessmentAiResponse;

public interface AiTutorClient {

    AiTutorResult ask(AiTutorRequest request);

    AssessmentAiResponse assess(AssessmentAiRequest request);
}
