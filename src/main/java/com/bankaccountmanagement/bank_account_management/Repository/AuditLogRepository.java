package com.bankaccountmanagement.bank_account_management.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bankaccountmanagement.bank_account_management.Model.AuditLog;

@Repository
public interface  AuditLogRepository extends JpaRepository<AuditLog, Long> {
     List<AuditLog> findByUserId(Long userId);
    
    List<AuditLog> findByAction(String action);
    
    List<AuditLog> findByStatus(String status);
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :start AND :end")
    List<AuditLog> findByUserIdAndTimestampBetween(Long userId, LocalDateTime start, LocalDateTime end);

}