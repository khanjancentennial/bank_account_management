package com.bankaccountmanagement.bank_account_management.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private String accountNumber;
    private String phoneNumber;
    private Double balance;

    public UserResponseDto(Long id, 
    String username, 
    String email,
    String firstName, 
    String lastName, 
    String address, 
    String accountNumber, 
    String phoneNumber, 
    Double balance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.accountNumber = accountNumber;
        this.phoneNumber = phoneNumber;
        this.balance = balance;
    }
}

