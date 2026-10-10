
package com.example.aigateway.exception;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimit(
            RateLimitExceededException ex
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(
                HttpHeaders.RETRY_AFTER,
                String.valueOf(ex.getRetryAfterSeconds())
        );

        return new ResponseEntity<>(
                Map.of(
                        "status", 429,
                        "error", "Too Many Requests",
                        "message", "Rate limit exceeded",
                        "retryAfterSeconds",
                        ex.getRetryAfterSeconds()
                ),
                headers,
                HttpStatus.TOO_MANY_REQUESTS
        );
    }
}
