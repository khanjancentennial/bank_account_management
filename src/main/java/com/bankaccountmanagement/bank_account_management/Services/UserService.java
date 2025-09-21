package com.bankaccountmanagement.bank_account_management.Services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bankaccountmanagement.bank_account_management.Model.UserModel;
import com.bankaccountmanagement.bank_account_management.Repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private final String BANK_ID = "01234";
    private final String BRANCH_ID = "5678";

    @Autowired
    public void setPasswordEncoder(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public UserModel registerNewUser(String username, 
    String password, 
    String email, 
    String firstName, 
    String lastName,
    String address, 
    String accountNumber,
    String phoneNumber,
    Double balance

    ) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        UserModel newUser = new UserModel();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setAddress(address);
        newUser.setAccountNumber(generateAccountNumber());
        newUser.setPhoneNumber(phoneNumber);
        newUser.setBalance(balance);
        return userRepository.save(newUser);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private String generateAccountNumber() {
        // Get the last account number from the database
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
}

