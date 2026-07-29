package com.devmentor.ai.client;

import java.time.Duration;

public interface OllamaTransport {

    TransportResponse post(String url, String body, Duration timeout);

    record TransportResponse(int statusCode, String body) {
    }
}
