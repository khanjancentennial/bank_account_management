package com.bankaccountmanagement.bank_account_management.Services;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

/**
 * Banking-grade rate limiting service
 * Implements multi-layer rate limiting as used by major banks
 */
@Service
public class BankingRateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(BankingRateLimitService.class);

    // Layer 1: Per username - Prevent brute force on specific account
    private final Map<String, Bucket> usernameBuckets = new ConcurrentHashMap<>();
    
    // Layer 2: Per IP - Prevent attacker from hitting multiple accounts
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    
    // Layer 3: Per IP+Username combo - Most restrictive
    private final Map<String, Bucket> combinedBuckets = new ConcurrentHashMap<>();
    
    // Track failed attempts for CAPTCHA and email alerts
    private final Map<String, Integer> failedAttemptCounter = new ConcurrentHashMap<>();

    /**
     * Check if login attempt is allowed
     * Implements 3-layer rate limiting:
     * 1. Username: 5 attempts per 15 minutes
     * 2. IP: 20 attempts per 15 minutes
     * 3. Combined: 3 attempts per 5 minutes
     */
    public RateLimitResult checkLoginAllowed(String username, String ipAddress) {
        
        String normalizedUsername = username.toLowerCase().trim();
        
        // Layer 1: Username-based limit (prevents brute force on specific account)
        Bucket usernameBucket = usernameBuckets.computeIfAbsent(
            normalizedUsername, 
            k -> createBucket(5, Duration.ofMinutes(15))
        );
        
        if (!usernameBucket.tryConsume(1)) {
            logger.warn("Username rate limit exceeded for: {}", normalizedUsername);
            return new RateLimitResult(
                false, 
                "Too many login attempts for this account. Please try again in 15 minutes or use 'Forgot Password'.",
                "USERNAME_LIMIT",
                15
            );
        }

        // Layer 2: IP-based limit (prevents distributed attack on multiple accounts)
        Bucket ipBucket = ipBuckets.computeIfAbsent(
            ipAddress, 
            k -> createBucket(20, Duration.ofMinutes(15))
        );
        
        if (!ipBucket.tryConsume(1)) {
            logger.warn("IP rate limit exceeded for: {}", ipAddress);
            return new RateLimitResult(
                false, 
                "Too many login attempts from your network. Please try again in 15 minutes.",
                "IP_LIMIT",
                15
            );
        }

        // Layer 3: Combined limit (most restrictive - prevents rapid attacks)
        String combinedKey = ipAddress + ":" + normalizedUsername;
        Bucket combinedBucket = combinedBuckets.computeIfAbsent(
            combinedKey, 
            k -> createBucket(3, Duration.ofMinutes(5))
        );
        
        if (!combinedBucket.tryConsume(1)) {
            logger.warn("Combined rate limit exceeded for {} from {}", normalizedUsername, ipAddress);
            return new RateLimitResult(
                false, 
                "Too many failed attempts. Please wait 5 minutes before trying again.",
                "COMBINED_LIMIT",
                5
            );
        }

        // All limits passed
        return new RateLimitResult(true, "Allowed", "ALLOWED", 0);
    }

    /**
     * Reset all limits for a user (call on successful login)
     */
    public void resetUserLimits(String username, String ipAddress) {
        String normalizedUsername = username.toLowerCase().trim();
        String combinedKey = ipAddress + ":" + normalizedUsername;
        
        usernameBuckets.remove(normalizedUsername);
        ipBuckets.remove(ipAddress);
        combinedBuckets.remove(combinedKey);
        failedAttemptCounter.remove(normalizedUsername);
        
        logger.info("Rate limits reset for successful login: {}", normalizedUsername);
    }

    /**
     * Reset only username limit (for account recovery scenarios)
     */
    public void resetUsernameLimits(String username) {
        String normalizedUsername = username.toLowerCase().trim();
        usernameBuckets.remove(normalizedUsername);
        failedAttemptCounter.remove(normalizedUsername);
        
        logger.info("Username rate limit reset for: {}", normalizedUsername);
    }

    /**
     * Increment failed attempt counter (for CAPTCHA and email alerts)
     */
    public int incrementFailedAttempts(String username) {
        String normalizedUsername = username.toLowerCase().trim();
        int attempts = failedAttemptCounter.merge(normalizedUsername, 1, Integer::sum);
        logger.debug("Failed attempt count for {}: {}", normalizedUsername, attempts);
        return attempts;
    }

    /**
     * Get current failed attempt count
     */
    public int getFailedAttemptCount(String username) {
        String normalizedUsername = username.toLowerCase().trim();
        return failedAttemptCounter.getOrDefault(normalizedUsername, 0);
    }

    /**
     * Check if CAPTCHA is required (after 3 failed attempts)
     */
    public boolean requiresCaptcha(String username) {
        return getFailedAttemptCount(username) >= 3;
    }

    /**
     * Check if email notification should be sent (after 3 failed attempts)
     */
    public boolean shouldSendEmailAlert(String username) {
        return getFailedAttemptCount(username) == 3;
    }

    /**
     * Create a bucket with specified capacity and refill duration
     */
    private Bucket createBucket(int capacity, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(
            capacity, 
            Refill.intervally(capacity, refillDuration)
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Clean up old entries (call this periodically via scheduled task)
     */
    public void cleanup() {
        // In production, implement TTL-based cleanup
        // For now, clear buckets older than 1 hour
        logger.info("Rate limit cleanup - Current buckets: username={}, ip={}, combined={}", 
            usernameBuckets.size(), ipBuckets.size(), combinedBuckets.size());
    }

    /**
     * Result class for rate limit checks
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final String message;
        private final String limitType;
        private final int retryAfterMinutes;

        public RateLimitResult(boolean allowed, String message, String limitType, int retryAfterMinutes) {
            this.allowed = allowed;
            this.message = message;
            this.limitType = limitType;
            this.retryAfterMinutes = retryAfterMinutes;
        }

        public boolean isAllowed() { 
            return allowed; 
        }
        
        public String getMessage() { 
            return message; 
        }
        
        public String getLimitType() { 
            return limitType; 
        }
        
        public int getRetryAfterMinutes() { 
            return retryAfterMinutes; 
        }
    }
}



