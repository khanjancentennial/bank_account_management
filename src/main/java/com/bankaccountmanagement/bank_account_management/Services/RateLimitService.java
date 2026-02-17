package com.bankaccountmanagement.bank_account_management.Services;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Service for implementing rate limiting using Token Bucket algorithm.
 * Prevents brute force attacks and API abuse.
 */
@Service
public class RateLimitService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Get or create a bucket for login attempts (5 attempts per 15 minutes)
     */
    public Bucket resolveLoginBucket(String key) {
        return cache.computeIfAbsent(key, k -> createLoginBucket());
    }

    /**
     * Get or create a bucket for general API calls (100 requests per minute)
     */
    public Bucket resolveApiBucket(String key) {
        return cache.computeIfAbsent(key, k -> createApiBucket());
    }

    /**
     * Get or create a bucket for transaction operations (20 per minute)
     */
    public Bucket resolveTransactionBucket(String key) {
        return cache.computeIfAbsent(key, k -> createTransactionBucket());
    }

    /**
     * Login bucket: 5 attempts per 15 minutes
     */
    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(15)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * API bucket: 100 requests per minute
     */
    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Transaction bucket: 20 transactions per minute (prevents rapid-fire attacks)
     */
    private Bucket createTransactionBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Clear rate limit for a specific key (useful after successful actions)
     */
    public void clearLimit(String key) {
        cache.remove(key);
    }
}
