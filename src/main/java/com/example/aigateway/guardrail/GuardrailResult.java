package com.example.aigateway.guardrail;

public record GuardrailResult(
        boolean allowed,
        String content,
        String reason
) {
    public static GuardrailResult allow(String content) {
        return new GuardrailResult(true, content, null);
    }

    public static GuardrailResult reject(String reason) {
        return new GuardrailResult(false, null, reason);
    }
}
