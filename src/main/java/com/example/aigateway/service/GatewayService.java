
package com.example.aigateway.service;

import com.example.aigateway.dto.ChatRequest;
import com.example.aigateway.dto.ChatResponse;
import com.example.aigateway.exception.RateLimitExceededException;
import com.example.aigateway.service.GuardrailService;
import com.example.aigateway.provider.LlmProvider;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GatewayService {

    private final Map<String, LlmProvider> providers;
    private final RedisCacheService cacheService;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final RedisRateLimitService rateLimitService;
    private final GuardrailService guardrailService;
    private final GatewayMetricsService metricsService;

    public GatewayService(
            List<LlmProvider> providers,
            RedisCacheService cacheService,
            CacheKeyGenerator cacheKeyGenerator,
            RedisRateLimitService rateLimitService,
            GuardrailService guardrailService,
            GatewayMetricsService metricsService
    ) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        LlmProvider::name,
                        Function.identity()
                ));

        this.cacheService = cacheService;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.rateLimitService = rateLimitService;
        this.guardrailService = guardrailService;
        this.metricsService = metricsService;
    }

    public ChatResponse chat(
            ChatRequest request,
            String clientApiKey
    ) {
        long startTime = System.nanoTime();

        LlmProvider provider = resolveProvider(request.provider());

        enforceRateLimit(clientApiKey);

        ChatRequest sanitizedRequest = sanitizeRequest(request);

        String cacheKey = generateCacheKey(
                clientApiKey,
                sanitizedRequest
        );

        Optional<String> cachedResponse = getCachedResponse(cacheKey);

        if (cachedResponse.isPresent()) {
            return buildResponse(
                    provider,
                    sanitizedRequest.model(),
                    cachedResponse.get(),
                    startTime,
                    true
            );
        }

        String generatedResponse = generateResponse(
                provider,
                sanitizedRequest
        );

        cacheResponse(cacheKey, generatedResponse);

        return buildResponse(
                provider,
                sanitizedRequest.model(),
                generatedResponse,
                startTime,
                false
        );
    }

    // --------------------------------------------------
    // Provider resolution
    // --------------------------------------------------

    private LlmProvider resolveProvider(String providerName) {
        LlmProvider provider = providers.get(providerName);

        if (provider == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported provider: " + providerName
            );
        }

        return provider;
    }

    // --------------------------------------------------
    // Rate limiting
    // --------------------------------------------------

    private void enforceRateLimit(String clientApiKey) {
        RedisRateLimitService.RateLimitResult result;

        try {
            result = rateLimitService.consume(clientApiKey);
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Rate limiting temporarily unavailable",
                    ex
            );
        }

        if (!result.allowed()) {
            metricsService.recordRateLimitRejection();
            throw new RateLimitExceededException(
                    result.retryAfterSeconds()
            );
        }
    }

    // --------------------------------------------------
    // Input guardrails
    // --------------------------------------------------

    private ChatRequest sanitizeRequest(ChatRequest request) {
        String sanitizedMessage =
                guardrailService.validateInput(request.message());

        return new ChatRequest(
                request.provider(),
                request.model(),
                sanitizedMessage
        );
    }

    // --------------------------------------------------
    // Cache key
    // --------------------------------------------------

    private String generateCacheKey(
            String clientApiKey,
            ChatRequest request
    ) {
        return cacheKeyGenerator.generate(clientApiKey, request);
    }

    // --------------------------------------------------
    // Redis cache lookup
    // --------------------------------------------------

    private Optional<String> getCachedResponse(String cacheKey) {
        Optional<String> cached = cacheService.get(cacheKey);

        metricsService.recordCacheHit(cached.isPresent());

        return cached.map(guardrailService::validateOutput);
    }

    // --------------------------------------------------
    // LLM generation + output guardrails
    // --------------------------------------------------

    private String generateResponse(
            LlmProvider provider,
            ChatRequest request
    ) {
        long start = System.nanoTime();

        try {
            String generated = provider.generate(
                    request.model(),
                    request.message()
            );

            return guardrailService.validateOutput(generated);
        } finally {
            metricsService.recordModelDuration(
                    provider.name(),
                    request.model(),
                    Duration.ofNanos(System.nanoTime() - start)
            );
        }
    }

    // --------------------------------------------------
    // Redis cache write
    // --------------------------------------------------

    private void cacheResponse(
            String cacheKey,
            String response
    ) {
        cacheService.put(cacheKey, response);
    }

    // --------------------------------------------------
    // Response construction
    // --------------------------------------------------

    private ChatResponse buildResponse(
            LlmProvider provider,
            String model,
            String content,
            long startTime,
            boolean cached
    ) {
        return new ChatResponse(
                provider.name(),
                model,
                content,
                elapsedMillis(startTime),
                cached
        );
    }

    private long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }
}
