package com.bankaccountmanagement.bank_account_management.Services;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bankaccountmanagement.bank_account_management.Model.AuditLog;
import com.bankaccountmanagement.bank_account_management.Repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {
private final AuditLogRepository auditLogRepository;
    private static final Logger logger = Logger.getLogger(AuditService.class.getName());

    /**
     * Log a user action asynchronously
     */
    @Async
    public void logAction(Long userId, String username, String action, String status, String details) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .status(status)
                    .details(details)
                    .ipAddress(request != null ? getClientIpAddress(request) : "UNKNOWN")
                    .userAgent(request != null ? request.getHeader("User-Agent") : "UNKNOWN")
                    .build();
            
            auditLogRepository.save(auditLog);
            logger.log(Level.INFO, "Audit log created: {0} - {1} - {2}", 
                new Object[]{username, action, status});
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create audit log: {0}", e.getMessage());
        }
    }

    /**
     * Log a financial transaction
     */
    @Async
    public void logTransaction(Long userId, String username, String action, String accountNumber, 
                               Double amount, String status, String details) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .accountNumber(accountNumber)
                    .amount(amount)
                    .status(status)
                    .details(details)
                    .ipAddress(request != null ? getClientIpAddress(request) : "UNKNOWN")
                    .userAgent(request != null ? request.getHeader("User-Agent") : "UNKNOWN")
                    .build();
            
            auditLogRepository.save(auditLog);
            logger.log(Level.INFO, "Transaction audit log: {0} - {1} - Amount: {2}", 
                new Object[]{username, action, amount});
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create transaction audit log: {0}", e.getMessage());
        }
    }

    /**
     * Log failed attempts with error messages
     */
    @Async
    public void logFailure(String username, String action, String errorMessage) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .status("FAILURE")
                    .errorMessage(errorMessage)
                    .ipAddress(request != null ? getClientIpAddress(request) : "UNKNOWN")
                    .userAgent(request != null ? request.getHeader("User-Agent") : "UNKNOWN")
                    .build();
            
            auditLogRepository.save(auditLog);
            logger.log(Level.WARNING, "Failed action logged: {0} - {1}", 
                new Object[]{username, action});
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create failure audit log: {0}", e.getMessage());
        }
    }

    /**
     * Extract real client IP address (handles proxy headers)
     */
    private String getClientIpAddress(HttpServletRequest request) {
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

    /**
     * Get current HTTP request from context
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
