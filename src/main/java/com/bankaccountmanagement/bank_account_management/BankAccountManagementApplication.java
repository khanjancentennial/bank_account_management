package com.bankaccountmanagement.bank_account_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Enable asynchronous processing for audit logging and other background tasks
public class BankAccountManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankAccountManagementApplication.class, args);
	}

}
