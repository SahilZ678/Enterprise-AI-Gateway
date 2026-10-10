package com.example.aigateway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class GuardrailRejectedException
        extends ResponseStatusException {

    public GuardrailRejectedException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
