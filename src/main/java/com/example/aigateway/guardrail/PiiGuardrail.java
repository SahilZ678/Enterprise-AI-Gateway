package com.example.aigateway.guardrail;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PiiGuardrail implements InputGuardrail {

    private static final Pattern EMAIL = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern SSN = Pattern.compile(
            "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );

    @Override
    public GuardrailResult validate(String input) {

        String sanitized = EMAIL.matcher(input)
                .replaceAll("[REDACTED_EMAIL]");

        sanitized = SSN.matcher(sanitized)
                .replaceAll("[REDACTED_SSN]");

        return GuardrailResult.allow(sanitized);
    }
}
