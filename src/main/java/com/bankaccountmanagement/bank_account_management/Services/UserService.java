package com.bankaccountmanagement.bank_account_management.Services;

// import java.util.Optional;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.annotation.Lazy;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Service;

// import com.bankaccountmanagement.bank_account_management.Model.UserModel;
// import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;

// import lombok.RequiredArgsConstructor;

// @Service
// @RequiredArgsConstructor
// public class UserService implements UserDetailsService {

//     private final UserRepository userRepository;
//     private PasswordEncoder passwordEncoder;
//     private final String BANK_ID = "01234";
//     private final String BRANCH_ID = "5678";

//     @Autowired
//     public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
//         this.passwordEncoder = passwordEncoder;
//     }

//     public UserModel registerNewUser(String username, 
//     String password, 
//     String email, 
//     String firstName, 
//     String lastName,
//     String address, 
//     String accountNumber,
//     String phoneNumber,
//     Double balance

//     ) {
//         if (userRepository.existsByUsername(username)) {
//             throw new RuntimeException("Username already exists");
//         }
//         UserModel newUser = new UserModel();
//         newUser.setUsername(username);
//         newUser.setPassword(passwordEncoder.encode(password));
//         newUser.setEmail(email);
//         newUser.setFirstName(firstName);
//         newUser.setLastName(lastName);
//         newUser.setAddress(address);
//         newUser.setAccountNumber(generateAccountNumber());
//         newUser.setPhoneNumber(phoneNumber);
//         newUser.setBalance(balance);
//         return userRepository.save(newUser);
//     }

//     @Override
//     public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//         return userRepository.findByUsername(username)
//                 .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
//     }

//     private String generateAccountNumber() {
//         // Get the last account number from the database
//         Optional<UserModel> lastUser = userRepository.findTopByOrderByAccountNumberDesc();
        
//         long lastIncrementalNumber = 0;
//         if (lastUser.isPresent()) {
//             String lastAccountNumber = lastUser.get().getAccountNumber();
//             String incrementalPart = lastAccountNumber.substring(lastAccountNumber.length() - 5);
//             lastIncrementalNumber = Long.parseLong(incrementalPart);
//         }

//         long nextIncrementalNumber = lastIncrementalNumber + 1;
//         String formattedNumber = String.format("%05d", nextIncrementalNumber);

//         return BANK_ID + BRANCH_ID + formattedNumber;
//     }
// }


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bankaccountmanagement.bank_account_management.Model.Role;
import com.bankaccountmanagement.bank_account_management.Model.UserModel;
import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private PasswordEncoder passwordEncoder;
    private final String BANK_ID = "01234";
    private final String BRANCH_ID = "5678";

    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user with validation and audit logging
     */
    public UserModel registerNewUser(String username, 
                                     String password, 
                                     String email, 
                                     String firstName, 
                                     String lastName,
                                     String address, 
                                     String accountNumber,
                                     String phoneNumber,
                                     Double balance) {
        // Validation
        if (userRepository.existsByUsername(username)) {
            auditService.logFailure(username, "REGISTRATION", "Username already exists");
            throw new RuntimeException("Username already exists");
        }

        // Input validation
        validateUserInput(username, password, email, phoneNumber);
        
        UserModel newUser = new UserModel();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setAddress(address);
        newUser.setAccountNumber(generateAccountNumber());
        newUser.setPhoneNumber(phoneNumber);
        newUser.setBalance(balance != null ? balance : 0.0);
        newUser.setRole(Role.USER); // Default role
        newUser.setAccountLocked(false);
        newUser.setFailedLoginAttempts(0);
        
        UserModel savedUser = userRepository.save(newUser);
        
        // Audit log
        auditService.logAction(
            savedUser.getId(),
            savedUser.getUsername(),
            "USER_REGISTRATION",
            "SUCCESS",
            "New user registered with account: " + savedUser.getAccountNumber()
        );
        
        return savedUser;
    }

    /**
     * Register an admin user (only for initial setup or by other admins)
     */
    public UserModel registerAdmin(String username, 
                                   String password, 
                                   String email, 
                                   String firstName, 
                                   String lastName,
                                   String phoneNumber) {
        UserModel admin = registerNewUser(
            username, password, email, firstName, lastName, 
            "Admin Office", null, phoneNumber, 0.0
        );
        admin.setRole(Role.ADMIN);
        UserModel savedAdmin = userRepository.save(admin);
        
        auditService.logAction(
            savedAdmin.getId(),
            savedAdmin.getUsername(),
            "ADMIN_REGISTRATION",
            "SUCCESS",
            "Admin user created"
        );
        
        return savedAdmin;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Handle failed login attempt
     */
    public void recordFailedLoginAttempt(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.incrementFailedAttempts();
            userRepository.save(user);
            
            auditService.logAction(
                user.getId(),
                username,
                "LOGIN_FAILED",
                "FAILURE",
                "Failed login attempt. Total attempts: " + user.getFailedLoginAttempts()
            );
            
            if (user.isAccountLocked()) {
                auditService.logAction(
                    user.getId(),
                    username,
                    "ACCOUNT_LOCKED",
                    "SUCCESS",
                    "Account locked due to multiple failed login attempts"
                );
            }
        });
    }

    /**
     * Handle successful login
     */
    public void recordSuccessfulLogin(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.resetFailedAttempts();
            userRepository.save(user);
            
            auditService.logAction(
                user.getId(),
                username,
                "LOGIN_SUCCESS",
                "SUCCESS",
                "User logged in successfully"
            );
        });
    }

    /**
     * Generate unique account number
     */
    private String generateAccountNumber() {
        Optional<UserModel> lastUser = userRepository.findTopByOrderByAccountNumberDesc();
        
        long lastIncrementalNumber = 0;
        if (lastUser.isPresent()) {
            String lastAccountNumber = lastUser.get().getAccountNumber();
            String incrementalPart = lastAccountNumber.substring(lastAccountNumber.length() - 5);
            lastIncrementalNumber = Long.parseLong(incrementalPart);
        }

        long nextIncrementalNumber = lastIncrementalNumber + 1;
        String formattedNumber = String.format("%05d", nextIncrementalNumber);

        return BANK_ID + BRANCH_ID + formattedNumber;
    }

    /**
     * Validate user input
     */
    private void validateUserInput(String username, String password, String email, String phoneNumber) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (phoneNumber == null || !phoneNumber.matches("^\\+?[0-9]{10,15}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
}