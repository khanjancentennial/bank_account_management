package com.bankaccountmanagement.bank_account_management.Execptions;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
