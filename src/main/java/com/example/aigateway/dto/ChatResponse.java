package com.example.aigateway.dto;

public record ChatResponse(
        String provider,
        String model,
        String response,
        long latencyMs,
        boolean cached
) {}
