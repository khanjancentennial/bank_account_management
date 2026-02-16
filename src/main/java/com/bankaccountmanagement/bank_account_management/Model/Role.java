package com.bankaccountmanagement.bank_account_management.Model;

/**
 * Enum representing user roles in the banking system.
 * ADMIN: Full access to all operations including user management
 * USER: Standard customer with access to their own account operations
 * AUDITOR: Read-only access for compliance and auditing purposes
 */

public enum Role {
    USER,
    ADMIN,
    AUDITOR
}
