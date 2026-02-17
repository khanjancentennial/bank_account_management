package com.bankaccountmanagement.bank_account_management.Config;
import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");
        
        // Enable XSS protection in browsers
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Force HTTPS (only in production - comment out in development)
        // response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");
        
        // Referrer policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions policy (restrict features)
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=()");

        filterChain.doFilter(request, response);
    }

}
