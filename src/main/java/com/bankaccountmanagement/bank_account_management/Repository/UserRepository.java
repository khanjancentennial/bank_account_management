package com.bankaccountmanagement.bank_account_management.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.bankaccountmanagement.bank_account_management.Model.UserModel;

public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByUsername(String username);
    boolean existsByUsername(String username);

    Optional<UserModel> findByAccountNumber(String accountNumber);

    // Custom query to find the user with the highest account number
    @Query(value = "SELECT * FROM users ORDER BY account_number DESC LIMIT 1", nativeQuery = true)
    Optional<UserModel> findTopByOrderByAccountNumberDesc();
}