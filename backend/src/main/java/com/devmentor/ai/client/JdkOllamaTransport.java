package com.devmentor.ai.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.ai.mode", havingValue = "ollama")
public class JdkOllamaTransport implements OllamaTransport {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public TransportResponse post(String url, String body, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            return new TransportResponse(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new AiClientException("로컬 AI 서버에 연결하지 못했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiClientException("로컬 AI 요청이 중단되었습니다.", exception);
        }
    }
}
