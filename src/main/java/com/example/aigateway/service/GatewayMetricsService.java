package com.example.aigateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GatewayMetricsService {

    private final MeterRegistry registry;

    public GatewayMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCacheHit(boolean hit) {
        Counter.builder("ai.gateway.cache.requests")
                .description("AI response cache requests")
                .tag("result", hit ? "hit" : "miss")
                .register(registry)
                .increment();
    }

    public void recordRateLimitRejection() {
        registry.counter(
                "ai.gateway.rate.limit.rejections"
        ).increment();
    }

    public void recordGuardrailRejection(String stage) {
        registry.counter(
                "ai.gateway.guardrail.rejections",
                "stage", stage
        ).increment();
    }

    public void recordModelDuration(
            String provider,
            String model,
            Duration duration
    ) {
        Timer.builder("ai.gateway.model.duration")
                .description("LLM inference duration")
                .tag("provider", provider)
                .tag("model", model)
                .publishPercentileHistogram()
                .register(registry)
                .record(duration);
    }

    public void recordRequest(
            String provider,
            String result,
            Duration duration
    ) {
        Timer.builder("ai.gateway.request.duration")
                .tag("provider", provider)
                .tag("result", result)
                .publishPercentileHistogram()
                .register(registry)
                .record(duration);
    }
}
