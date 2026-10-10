package com.example.aigateway.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class OllamaProvider implements LlmProvider {

    private final RestClient client;
    private final Set<String> allowedModels;

    public OllamaProvider(
            RestClient.Builder builder,
            @Value("${ai.ollama.base-url}") String baseUrl,
            @Value("${ai.ollama.allowed-models}") String models) {
        this.client = builder
                .baseUrl(baseUrl)
                .build();

        this.allowedModels = Set.of(models.split(","));
    }

    @Override
    public String name() {
        return "ollama";
    }

    @Override
    public String generate(String model, String message) {
        if (!allowedModels.contains(model)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Model not allowed"
            );
        }

        var request = Map.of(
                "model", model,
                "stream", false,
                "think", false,
                "keep_alive", "10m",
                "options", Map.of(
                        "num_predict", 150,
                        "temperature", 0.3
                ),
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", message
                        )
                )
        );

        OllamaResponse response = client.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null ||
                response.message() == null ||
                response.message().content() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response from Ollama"
            );
        }

        return response.message().content();
    }

    public record OllamaResponse(
            OllamaMessage message ) {}

    public record OllamaMessage(
            String role,
            String content ) {}
}
