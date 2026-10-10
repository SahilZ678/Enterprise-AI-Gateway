package com.example.aigateway.provider;

public interface LlmProvider {

    String name();

    String generate(
            String model,
            String message
    );
}
