
package com.example.aigateway.service;

import java.util.ArrayList;
import java.util.List;

import com.example.aigateway.exception.GuardrailRejectedException;
import com.example.aigateway.guardrail.GuardrailResult;
import com.example.aigateway.guardrail.InputGuardrail;
import com.example.aigateway.guardrail.OutputGuardrail;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

@Service
public class GuardrailService {

    private final List<InputGuardrail> inputGuardrails;
    private final List<OutputGuardrail> outputGuardrails;

    public GuardrailService(List<InputGuardrail> inputGuardrails, List<OutputGuardrail> outputGuardrails) {
        this.inputGuardrails = new ArrayList<>(inputGuardrails);
        this.outputGuardrails = new ArrayList<>(outputGuardrails);

        AnnotationAwareOrderComparator.sort(this.inputGuardrails);
        AnnotationAwareOrderComparator.sort(this.outputGuardrails);
    }

    public String validateInput(String input) {

        String content = input;

        for (InputGuardrail guardrail : inputGuardrails) {
            GuardrailResult result = guardrail.validate(content);
            if (!result.allowed()) {
                throw new GuardrailRejectedException(
                        result.reason());
            }
            content = result.content();
        }
        return content;
    }

    public String validateOutput(String output) {
        if (output == null) {
            throw new GuardrailRejectedException(
                    "Model returned an invalid response"
            );
        }

        String content = output;

        for (OutputGuardrail guardrail : outputGuardrails) {
            GuardrailResult result = guardrail.validate(content);

            if (!result.allowed()) {
                throw new GuardrailRejectedException(
                        result.reason()
                );
            }

            content = result.content();
        }

        return content;
    }
}
