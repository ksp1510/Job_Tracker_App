package com.jobtracker.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SECURITY: Rate limiting service to prevent brute force attacks
 */
@Service
public class RateLimitService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Get bucket for IP address with rate limiting
     * - 5 login attempts per minute per IP
     * - 10 registration attempts per hour per IP
     */
    public Bucket resolveBucketForLogin(String key) {
        return cache.computeIfAbsent(key, k -> createLoginBucket());
    }

    public Bucket resolveBucketForRegistration(String key) {
        return cache.computeIfAbsent(key, k -> createRegistrationBucket());
    }

    private Bucket createLoginBucket() {
        // 5 attempts per minute
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createRegistrationBucket() {
        // 10 attempts per hour
        Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * General API rate limiting: 100 requests per minute
     */
    public Bucket resolveBucketForApi(String key) {
        return cache.computeIfAbsent(key, k -> createApiBucket());
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
