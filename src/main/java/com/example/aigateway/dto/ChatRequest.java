package com.example.aigateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank String provider,
        @NotBlank String model,
        @NotBlank
        @Size(max = 16000)
        String message
) {}
