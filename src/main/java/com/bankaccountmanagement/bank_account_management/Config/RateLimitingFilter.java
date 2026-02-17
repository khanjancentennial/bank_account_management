package com.bankaccountmanagement.bank_account_management.Config;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bankaccountmanagement.bank_account_management.Services.RateLimitService;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filter to enforce rate limiting on API endpoints.
 * Different limits for login, transactions, and general API calls.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter{
    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientId = getClientIdentifier(request);
        
        Bucket bucket;
        
        // Different rate limits for different endpoints
        if (path.contains("/api/auth/login")) {
            bucket = rateLimitService.resolveLoginBucket(clientId + ":login");
        } else if (path.contains("/api/transactions")) {
            bucket = rateLimitService.resolveTransactionBucket(clientId + ":transaction");
        } else {
            bucket = rateLimitService.resolveApiBucket(clientId + ":api");
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.getWriter().write("{ \"error\": \"Too many requests. Please try again in " + waitForRefill + " seconds.\" }");
        }
    }

    /**
     * Get client identifier (IP address or username if authenticated)
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get IP from proxy headers first
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

}
