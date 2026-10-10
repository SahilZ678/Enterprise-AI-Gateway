package com.example.aigateway.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final String apiKey;

    public ApiKeyFilter(
            @Value("${gateway.api-key}") String apiKey
    ) {
        this.apiKey = apiKey;

        System.out.println("Configured API key: " + apiKey);
        System.out.println("Configured API key length: " + apiKey.length());
        System.out.println("Configured API key matches expected: "
                + "my-local-dev-key".equals(apiKey));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        if (!request.getRequestURI()
                .startsWith("/api/v1/")) {
            chain.doFilter(request, response);
            return;
        }

        String supplied =
                request.getHeader("X-API-Key");

        boolean valid = supplied != null &&
                MessageDigest.isEqual(
                        supplied.getBytes(StandardCharsets.UTF_8),
                        apiKey.getBytes(StandardCharsets.UTF_8)
                );

        if (!valid) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid API key"
            );
            return;
        }

        chain.doFilter(request, response);
    }
}
