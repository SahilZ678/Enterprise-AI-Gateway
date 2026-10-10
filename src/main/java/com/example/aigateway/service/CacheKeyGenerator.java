
package com.example.aigateway.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import com.example.aigateway.dto.ChatRequest;

@Component
public class CacheKeyGenerator {

    private final ObjectMapper objectMapper;

    public CacheKeyGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generate(
            String clientApiKey,
            ChatRequest request
    ) {
        try {
            // Include everything that affects the output.
            // v1 is the cache schema/prompt version.
            String payload = objectMapper.writeValueAsString(
                    List.of(
                            "v1",
                            clientApiKey,
                            request.provider(),
                            request.model(),
                            request.message()
                    )
            );

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            return "ai:chat:v1:" +
                    HexFormat.of().formatHex(hash);

        } catch (JsonProcessingException |
                NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "Unable to generate cache key", e
            );
        }
    }
}
