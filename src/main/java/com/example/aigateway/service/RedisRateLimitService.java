package com.example.aigateway.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimitService {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;
    private final int capacity;
    private final double refillRate;

    public RedisRateLimitService(
            StringRedisTemplate redis,
            @Value("${gateway.rate-limit.capacity:10}")
                    int capacity,
            @Value("${gateway.rate-limit.refill-tokens:10}")
                    int refillTokens,
            @Value("${gateway.rate-limit.refill-period:60s}")
                    Duration refillPeriod
    ) {
        if (capacity <= 0 || refillTokens <= 0 ||
                refillPeriod.isZero() ||
                refillPeriod.isNegative()) {
            throw new IllegalArgumentException(
                    "Invalid rate limit configuration"
            );
        }

        this.redis = redis;
        this.capacity = capacity;
        this.refillRate =
                refillTokens / (double) refillPeriod.toNanos()
                        * 1_000_000_000.0;

        this.script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(
                "scripts/token-bucket.lua"
        ));
        script.setResultType(List.class);
    }

    public RateLimitResult consume(String clientId) {
        String key = "ai:ratelimit:v1:" + sha256(clientId);

        List<?> result = redis.execute(
                script,
                List.of(key),
                String.valueOf(capacity),
                String.valueOf(refillRate)
        );

        if (result == null || result.size() != 3) {
            throw new IllegalStateException(
                    "Invalid Redis rate-limit result"
            );
        }

        return new RateLimitResult(
                ((Number) result.get(0)).longValue() == 1,
                ((Number) result.get(1)).longValue(),
                ((Number) result.get(2)).longValue()
        );
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record RateLimitResult(
            boolean allowed,
            long remaining,
            long retryAfterSeconds
    ) {}
}
