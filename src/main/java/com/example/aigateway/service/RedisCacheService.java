package com.example.aigateway.service;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheService {

    private static final Logger log =
            LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final boolean enabled;

    public RedisCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${gateway.cache.ttl:10m}") Duration ttl,
            @Value("${gateway.cache.enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;
        this.enabled = enabled;
    }


    public Optional<String> get(String key) {

        if (!enabled) {
            log.info("Redis caching is DISABLED");
            return Optional.empty();
        }

        try {
            String value =
                    redisTemplate.opsForValue().get(key);

            return Optional.ofNullable(value);

        } catch (Exception e) {
            log.error("Redis GET failed", e);
            return Optional.empty();
        }
    }

    public void put(String key, String value) {

        if (!enabled) {
            log.info("Redis caching is DISABLED");
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value, ttl);

        } catch (Exception e) {
            log.error("Redis SET failed", e);
        }
    }

}
