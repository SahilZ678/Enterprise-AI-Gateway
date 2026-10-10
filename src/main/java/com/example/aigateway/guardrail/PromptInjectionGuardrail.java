
package com.example.aigateway.guardrail;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class PromptInjectionGuardrail implements InputGuardrail {

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile(
                    "ignore\\s+(all\\s+)?(previous|prior)\\s+instructions",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "reveal\\s+(your\\s+)?(system|developer)\\s+prompt",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "you\\s+are\\s+now\\s+in\\s+developer\\s+mode",
                    Pattern.CASE_INSENSITIVE
            )
    );

    @Override
    public GuardrailResult validate(String input) {
        boolean suspicious = PATTERNS.stream()
                .anyMatch(p -> p.matcher(input).find());

        if (suspicious) {
            return GuardrailResult.reject(
                    "Potential prompt injection detected"
            );
        }

        return GuardrailResult.allow(input);
    }
}
