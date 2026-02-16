package com.bankaccountmanagement.bank_account_management.Model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "action", nullable = false, length = 100)
    private String action; // LOGIN, LOGOUT, DEPOSIT, WITHDRAWAL, TRANSFER, ACCOUNT_CREATION, etc.

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILURE, BLOCKED

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON or additional context

    @Column(name = "amount")
    private Double amount; // For financial transactions

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
