package com.bankaccountmanagement.bank_account_management.DTO;

import lombok.Data;

@Data
public class RegistrationDTO {
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private String accountNumber;
    private String phoneNumber;
    private Double balance;
}