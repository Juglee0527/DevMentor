package com.devmentor.ai.client;

import java.time.Duration;

public interface OpenAiTransport {

    TransportResponse post(String url, String apiKey, String body, Duration timeout);

    record TransportResponse(int statusCode, String body) {
    }
}
