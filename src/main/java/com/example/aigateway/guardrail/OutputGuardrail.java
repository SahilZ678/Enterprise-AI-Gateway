package com.example.aigateway.guardrail;

public interface OutputGuardrail {
    GuardrailResult validate(String output);
}
